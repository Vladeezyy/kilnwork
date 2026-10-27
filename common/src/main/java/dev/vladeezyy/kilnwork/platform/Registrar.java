package dev.vladeezyy.kilnwork.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * The entries of one registry of one mod, registered by the loader: NeoForge in its RegisterEvent (through
 * {@code KilnworkNeoForge.register(modId, bus)}), Fabric right after the mod starts (through
 * {@code KilnworkFabric.register(modId)}), registry by registry in the order of {@link BuiltInRegistries#REGISTRY}, so
 * the suppliers see the same registered entries on both loaders. Replaces NeoForge's DeferredRegister.
 */
public final class Registrar<R> {
  private static final List<Registrar<?>> ALL = new ArrayList<>();

  private final String modId;
  private final ResourceKey<? extends Registry<R>> registry;
  private final List<Entry<R, ? extends R>> entries = new ArrayList<>();
  private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();

  private Registrar(String modId, ResourceKey<? extends Registry<R>> registry) {
    this.modId = modId;
    this.registry = registry;
  }

  public static <R> Registrar<R> create(ResourceKey<? extends Registry<R>> registry, String modId) {
    Registrar<R> registrar = new Registrar<>(modId, registry);
    synchronized (ALL) {
      ALL.add(registrar);
    }
    return registrar;
  }

  public <T extends R> Entry<R, T> register(String name, Supplier<? extends T> factory) {
    Entry<R, T> entry = new Entry<>(ResourceKey.create(registry, ResourceLocation.fromNamespaceAndPath(modId, name)), factory);
    entries.add(entry);
    return entry;
  }

  /** A data component type, as NeoForge's {@code DeferredRegister.DataComponents#registerComponentType} */
  @SuppressWarnings("unchecked")
  public <D> Entry<DataComponentType<?>, DataComponentType<D>> registerComponentType(String name, UnaryOperator<DataComponentType.Builder<D>> builder) {
    return ((Registrar<DataComponentType<?>>) (Registrar<?>) this).register(name, () -> builder.apply(DataComponentType.<D>builder()).build());
  }

  /** Renames an entry: worlds with the old id still load (NeoForge registry alias; Fabric has no aliases) */
  public void addAlias(String from, String to) {
    aliases.put(ResourceLocation.fromNamespaceAndPath(modId, from), ResourceLocation.fromNamespaceAndPath(modId, to));
  }

  public Map<ResourceLocation, ResourceLocation> aliases() {
    return Collections.unmodifiableMap(aliases);
  }

  public String modId() {
    return modId;
  }

  public ResourceKey<? extends Registry<R>> registryKey() {
    return registry;
  }

  public Collection<Entry<R, ? extends R>> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /** All registrars of the given mod */
  public static List<Registrar<?>> of(String modId) {
    synchronized (ALL) {
      return ALL.stream().filter(registrar -> registrar.modId.equals(modId)).toList();
    }
  }

  /** Creates the entry's value: the loader calls this once and registers the result */
  public static <R> R create(Entry<R, ? extends R> entry) {
    return entry.create();
  }

  /**
   * A registered entry; {@link #get()} works once the loader has registered it. It is also an {@link ItemLike} for
   * blocks and items (as NeoForge's DeferredBlock and DeferredItem).
   */
  public static final class Entry<R, T extends R> implements Supplier<T>, ItemLike {
    private final ResourceKey<R> key;
    private final Supplier<? extends T> factory;
    private volatile T value;
    private Holder<R> holder;

    private Entry(ResourceKey<R> key, Supplier<? extends T> factory) {
      this.key = key;
      this.factory = factory;
    }

    private synchronized T create() {
      if (value == null) {
        value = factory.get();
      }
      return value;
    }

    @Override
    public T get() {
      T result = value;
      if (result == null) {
        throw new IllegalStateException("Registry entry not registered yet: " + key.location());
      }
      return result;
    }

    @Override
    public Item asItem() {
      if (get() instanceof ItemLike like) {
        return like.asItem();
      }
      throw new IllegalStateException(key.location() + " is not a block or an item");
    }

    public boolean isBound() {
      return value != null;
    }

    public ResourceKey<R> getKey() {
      return key;
    }

    public ResourceLocation getId() {
      return key.location();
    }

    /** The registry holder, as NeoForge's DeferredHolder is one */
    @SuppressWarnings("unchecked")
    public Holder<R> holder() {
      if (holder == null) {
        Registry<R> reg = (Registry<R>) BuiltInRegistries.REGISTRY.get(key.registry());
        if (reg == null) {
          throw new IllegalStateException("Unknown registry " + key.registry());
        }
        holder = reg.getHolderOrThrow(key);
      }
      return holder;
    }
  }
}
