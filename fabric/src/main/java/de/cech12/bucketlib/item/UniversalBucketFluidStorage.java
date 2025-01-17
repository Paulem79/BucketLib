package de.cech12.bucketlib.item;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class UniversalBucketFluidStorage extends SingleFluidStorage {

    private final ContainerItemContext context;

    public UniversalBucketFluidStorage(ContainerItemContext context) {
        this.context = context;
        if (context.getItemVariant().hasNbt()) {
            readNbt(context.getItemVariant().getNbt());
        }
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return FluidConstants.BUCKET;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        ItemStack stack = context.getItemVariant().toStack();
        return this.variant.isBlank() && BucketLibUtil.isEmpty(stack)
                && (context.getItemVariant().getItem() instanceof UniversalBucketItem universalBucketItem
                && universalBucketItem.canHoldFluid(variant.getFluid()));
    }

    @Override
    protected boolean canExtract(FluidVariant variant) {
        ItemStack stack = context.getItemVariant().toStack();
        return this.variant.getFluid() == variant.getFluid() && !BucketLibUtil.containsEntityType(stack) && !BucketLibUtil.containsMilk(stack);
    }

    @Override
    public long insert(FluidVariant insertedVariant, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);
        if (maxAmount >= getCapacity() && (insertedVariant.equals(variant) || variant.isBlank()) && canInsert(insertedVariant)) {
            ItemStack stack = context.getItemVariant().toStack();
            CompoundTag tag = stack.getOrCreateTag();
            tag.put("variant", insertedVariant.toNbt());
            tag.putLong("amount", getCapacity());
            stack.setTag(tag);
            if (exchangeOrRemove(ItemVariant.of(stack), transaction)) {
                return getCapacity();
            }
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(extractedVariant, maxAmount);
        if (maxAmount >= amount && (extractedVariant.equals(variant)) && canExtract(extractedVariant)) {
            ItemStack stack = context.getItemVariant().toStack();
            if (stack.getItem() instanceof UniversalBucketItem bucketItem) {
                if (!bucketItem.isCracked(stack)) {
                    if (BucketLibUtil.containsContent(stack)) { //remove milk content tag
                        BucketLibUtil.removeContentNoCopy(stack, false);
                    }
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.remove("variant");
                    tag.remove("amount");
                    stack.setTag(tag);
                    BucketLibUtil.damageByOne(stack);
                } else {
                    stack.shrink(1);
                }
                if (exchangeOrRemove(ItemVariant.of(stack), transaction)) {
                    return amount;
                }
            }
        }
        return 0;
    }

    private boolean exchangeOrRemove(ItemVariant newVariant, TransactionContext transaction) {
        if (!newVariant.isBlank()) {
            return context.exchange(newVariant, 1, transaction) == 1;
        } else {
            return context.extract(context.getItemVariant(), 1, transaction) == 1;
        }
    }

}
