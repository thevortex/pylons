package net.permutated.pylons.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.permutated.pylons.util.Constants;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public abstract class AbstractPylonTile extends BlockEntity {

    protected AbstractPylonTile(BlockEntityType<?> tileEntityType, BlockPos pos, BlockState state) {
        super(tileEntityType, pos, state);
    }

    public static final int SLOTS = 9;

    protected final ItemStackHandler itemStackHandler = new PylonItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return AbstractPylonTile.this.isItemValid(stack);
        }
    };

    protected abstract boolean isItemValid(ItemStack stack);

    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemStackHandler);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && side == null) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    public void dropItems() {
        AbstractPylonTile.dropItems(level, worldPosition, itemStackHandler);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        handler.invalidate();
    }

    protected UUID owner = null;
    protected String ownerName = null;

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    private long lastTicked = 0L;

    public boolean canTick(final int every) {
        long gameTime = level != null ? level.getGameTime() : 0L;
        if (gameTime % every == 0 && gameTime != lastTicked) {
            lastTicked = gameTime;
            return true;
        } else {
            return false;
        }
    }

    public abstract void tick();

    @SuppressWarnings("java:S1172") // unused arguments are required
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof AbstractPylonTile pylonTile) {
            pylonTile.tick();
        }
    }

    protected static void dropItems(@Nullable Level world, BlockPos pos, IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            ItemStack itemstack = itemHandler.getStackInSlot(i);

            if (itemstack.getCount() > 0 && world != null) {
                Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), itemstack);
            }
        }
    }

    /**
     * Serialize data to be sent to the GUI on the client.
     *
     * Overrides MUST call the super method first to ensure correct deserialization.
     * @param packetBuffer the packet ready to be filled
     */
    public void updateContainer(FriendlyByteBuf packetBuffer) {
        String lastKnown = UsernameCache.getLastKnownUsername(owner);
        String username = StringUtils.defaultString(lastKnown, Constants.UNKNOWN);

        packetBuffer.writeBlockPos(worldPosition);
        packetBuffer.writeInt(username.length());
        packetBuffer.writeUtf(username);
    }

    // Save TE data to disk
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put(Constants.NBT.INV, itemStackHandler.serializeNBT());
        writeOwner(tag);
    }

    // Write TE data to a provided CompoundNBT
    private void writeOwner(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID(Constants.NBT.OWNER, owner);
        }
    }

    // Load TE data from disk
    @Override
    public void load(CompoundTag tag) {
        itemStackHandler.deserializeNBT(tag.getCompound(Constants.NBT.INV));
        readOwner(tag);
        super.load(tag);
    }

    // Read TE data from a provided CompoundNBT
    private void readOwner(@Nullable CompoundTag tag) {
        if (tag != null && tag.hasUUID(Constants.NBT.OWNER)) {
            owner = tag.getUUID(Constants.NBT.OWNER);
        }
    }

    // Called whenever a client loads a new chunk
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeOwner(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@Nullable CompoundTag tag) {
        readOwner(tag);
    }

    // Called whenever a block update happens on the client
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    // Handles the update packet received from the server
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.handleUpdateTag(pkt.getTag());
    }

    public class PylonItemStackHandler extends ItemStackHandler {
        public PylonItemStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }
}
