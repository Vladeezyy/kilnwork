package dev.vladeezyy.kilnwork.registration;

import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registry entries for each value of an enum, such as one block per dye color.
 * @param <E>  Enum type
 * @param <T>  Entry type
 */
public class EnumObject<E extends Enum<E>, T> {
  private final Map<E, Supplier<? extends T>> map;

  private EnumObject(Map<E, Supplier<? extends T>> map) {
    this.map = map;
  }

  /** Gets the entry for the given value, throws if missing */
  public T get(E value) {
    Supplier<? extends T> holder = map.get(value);
    if (holder == null) {
      throw new IllegalArgumentException("No entry for " + value);
    }
    return holder.get();
  }

  /** Gets the entry for the given value, or null */
  @Nullable
  public T getOrNull(E value) {
    Supplier<? extends T> holder = map.get(value);
    return holder == null ? null : holder.get();
  }

  /** Gets the supplier for the given value */
  @Nullable
  public Supplier<? extends T> getSupplier(E value) {
    return map.get(value);
  }

  /** Values of the enum that have an entry */
  public Collection<E> keys() {
    return map.keySet();
  }

  /** All entries, in enum order */
  public List<T> values() {
    return map.values().stream().<T>map(Supplier::get).toList();
  }

  /** True if the given object is one of the entries */
  public boolean contains(@Nullable Object value) {
    for (Supplier<? extends T> holder : map.values()) {
      if (holder.get() == value) {
        return true;
      }
    }
    return false;
  }

  public void forEach(BiConsumer<E, ? super T> consumer) {
    map.forEach((key, holder) -> consumer.accept(key, holder.get()));
  }

  public void forEach(Consumer<? super T> consumer) {
    map.values().forEach(holder -> consumer.accept(holder.get()));
  }

  /** Items of all entries that have one, used for creative tabs */
  public void forEachItem(Consumer<ItemLike> consumer) {
    forEach(value -> {
      if (value instanceof ItemLike item) {
        consumer.accept(item);
      }
    });
  }

  /** Builder keeping the enum order */
  public static <E extends Enum<E>, T> Builder<E, T> builder(Class<E> clazz) {
    return new Builder<>(clazz);
  }

  public static class Builder<E extends Enum<E>, T> {
    private final Map<E, Supplier<? extends T>> map;

    private Builder(Class<E> clazz) {
      this.map = new EnumMap<>(clazz);
    }

    public Builder<E, T> put(E key, Supplier<? extends T> value) {
      if (map.putIfAbsent(key, value) != null) {
        throw new IllegalArgumentException("Duplicate key " + key);
      }
      return this;
    }

    public EnumObject<E, T> build() {
      return new EnumObject<>(map);
    }
  }
}
