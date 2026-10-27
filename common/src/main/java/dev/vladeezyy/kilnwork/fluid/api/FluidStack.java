package dev.vladeezyy.kilnwork.fluid.api;

import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluid with an amount in millibuckets and data components: the same as NeoForge's FluidStack, with the same codecs.
 * Loader neutral: NeoForge's fluid stacks and Fabric's fluid variants are converted at the edges (1 mB = 81 droplets).
 */
public final class FluidStack {

    /** Millibuckets in a bucket */
    public static final int BUCKET_VOLUME = 1000;

    public static final Codec<Holder<Fluid>> FLUID_NON_EMPTY_CODEC = BuiltInRegistries.FLUID.holderByNameCodec()
            .validate(holder -> holder.is(Fluids.EMPTY.builtInRegistryHolder())
                    ? com.mojang.serialization.DataResult.error(() -> "Fluid must not be minecraft:empty")
                    : com.mojang.serialization.DataResult.success(holder));
    public static final Codec<FluidStack> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::getFluidHolder),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(FluidStack::getComponentsPatch)
    ).apply(i, FluidStack::new)));
    public static final Codec<FluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(o -> o.orElse(FluidStack.EMPTY), s -> s.isEmpty() ? Optional.empty() : Optional.of(s));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FluidStack decode(RegistryFriendlyByteBuf buf) {
            int amount = buf.readVarInt();
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }
            Holder<Fluid> fluid = ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID).decode(buf);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new FluidStack(fluid, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
            if (stack.isEmpty()) {
                buf.writeVarInt(0);
            } else {
                buf.writeVarInt(stack.getAmount());
                ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID).encode(buf, stack.getFluidHolder());
                DataComponentPatch.STREAM_CODEC.encode(buf, stack.getComponentsPatch());
            }
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = OPTIONAL_STREAM_CODEC;

    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY.builtInRegistryHolder(), 0, DataComponentPatch.EMPTY);

    private final Holder<Fluid> fluid;
    private int amount;
    private final PatchedDataComponentMap components;

    public FluidStack(Holder<Fluid> fluid, int amount, DataComponentPatch patch) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch);
    }

    public FluidStack(Holder<Fluid> fluid, int amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    public FluidStack(Fluid fluid, int amount) {
        this(fluid.builtInRegistryHolder(), amount, DataComponentPatch.EMPTY);
    }

    private FluidStack(Holder<Fluid> fluid, int amount, PatchedDataComponentMap components) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = components;
    }

    public static Optional<FluidStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag).result();
    }

    public static FluidStack parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(lookupProvider, tag).orElse(EMPTY);
    }

    public Tag save(HolderLookup.Provider lookupProvider) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        }
        return CODEC.encodeStart(lookupProvider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), this).getOrThrow();
    }

    public Tag save(HolderLookup.Provider lookupProvider, Tag prefix) {
        return CODEC.encode(this, lookupProvider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), prefix).getOrThrow();
    }

    public Tag saveOptional(HolderLookup.Provider lookupProvider) {
        return isEmpty() ? new CompoundTag() : save(lookupProvider);
    }

    public PatchedDataComponentMap getComponents() {
        return components;
    }

    public DataComponentPatch getComponentsPatch() {
        return isEmpty() ? DataComponentPatch.EMPTY : components.asPatch();
    }

    public boolean isComponentsPatchEmpty() {
        return isEmpty() || components.asPatch().isEmpty();
    }

    @Nullable
    public <T> T get(DataComponentType<? extends T> type) {
        return components.get(type);
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
        return components.getOrDefault(type, defaultValue);
    }

    public boolean has(DataComponentType<?> type) {
        return components.has(type);
    }

    @Nullable
    public <T> T set(DataComponentType<? super T> type, @Nullable T component) {
        return components.set(type, component);
    }

    @Nullable
    public <T> T remove(DataComponentType<? extends T> type) {
        return components.remove(type);
    }

    public void applyComponents(DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    public boolean isEmpty() {
        return this == EMPTY || fluid.value() == Fluids.EMPTY || amount <= 0;
    }

    public FluidStack split(int amount) {
        int taken = Math.min(amount, this.amount);
        FluidStack result = copyWithAmount(taken);
        shrink(taken);
        return result;
    }

    public FluidStack copyAndClear() {
        if (isEmpty()) {
            return EMPTY;
        }
        FluidStack copy = copy();
        setAmount(0);
        return copy;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid.value();
    }

    public Holder<Fluid> getFluidHolder() {
        return fluid;
    }

    public boolean is(TagKey<Fluid> tag) {
        return getFluid().builtInRegistryHolder().is(tag);
    }

    public boolean is(Fluid fluid) {
        return getFluid() == fluid;
    }

    public boolean is(Holder<Fluid> holder) {
        return getFluid() == holder.value();
    }

    public boolean is(HolderSet<Fluid> holderSet) {
        return holderSet.contains(getFluid().builtInRegistryHolder());
    }

    public FluidStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        return new FluidStack(fluid, amount, components.copy());
    }

    public FluidStack copyWithAmount(int amount) {
        if (isEmpty() || amount <= 0) {
            return EMPTY;
        }
        FluidStack copy = copy();
        copy.setAmount(amount);
        return copy;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(int amount) {
        if (this == EMPTY) {
            throw new IllegalStateException("Can't modify the empty stack.");
        }
        this.amount = amount;
    }

    public void limitSize(int amount) {
        if (!isEmpty() && getAmount() > amount) {
            setAmount(amount);
        }
    }

    public void grow(int addedAmount) {
        setAmount(this.amount + addedAmount);
    }

    public void shrink(int removedAmount) {
        grow(-removedAmount);
    }

    public Component getHoverName() {
        return KilnworkPlatform.get().fluidName(this);
    }

    public String getDescriptionId() {
        return KilnworkPlatform.get().fluidDescriptionId(getFluid());
    }

    public String getTranslationKey() {
        return getDescriptionId();
    }

    public static boolean matches(FluidStack first, FluidStack second) {
        return first == second || first.getAmount() == second.getAmount() && isSameFluidSameComponents(first, second);
    }

    public static boolean isSameFluid(FluidStack first, FluidStack second) {
        return first.getFluid() == second.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack first, FluidStack second) {
        if (!isSameFluid(first, second)) {
            return false;
        }
        return first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components);
    }

    public static MapCodec<FluidStack> lenientOptionalFieldOf(String fieldName) {
        return CODEC.lenientOptionalFieldOf(fieldName).xmap(o -> o.orElse(EMPTY), s -> s.isEmpty() ? Optional.empty() : Optional.of(s));
    }

    public static int hashFluidAndComponents(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return 31 * stack.getFluid().hashCode() + stack.getComponents().hashCode();
    }

    @Override
    public String toString() {
        return getAmount() + " " + BuiltInRegistries.FLUID.getKey(getFluid());
    }
}
