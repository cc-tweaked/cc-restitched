/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.FakePlayer;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.OptionalInt;
import java.util.UUID;

public final class TurtlePlayer extends FakePlayer
{
    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString( "0d0c4ca0-4ff1-11e4-916c-0800200c9a66" ),
        "[ComputerCraft]"
    );

    private TurtlePlayer( ServerWorld world, GameProfile name )
    {
        super( world, name );
    }

    private static TurtlePlayer create( ITurtleAccess turtle )
    {
        ServerWorld world = (ServerWorld) turtle.getLevel();
        GameProfile profile = turtle.getOwningPlayer();

        TurtlePlayer player = new TurtlePlayer( world, getProfile( profile ) );
        player.setState( turtle );

        if( profile != null && profile.getId() != null )
        {
            // Constructing a player overrides the "active player" variable in advancements. As fake players cannot
            // get advancements, this prevents a normal player who has placed a turtle from getting advancements.
            // We try to locate the "actual" player and restore them.
            ServerPlayerEntity actualPlayer = world.getServer().getPlayerManager().getPlayer( profile.getId() );
            if( actualPlayer != null ) player.getAdvancementTracker().setOwner( actualPlayer );
        }

        return player;
    }

    private static GameProfile getProfile( @Nullable GameProfile profile )
    {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain brain) ) return create( access );

        TurtlePlayer player = brain.cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() )
            || player.getEntityWorld() != access.getLevel() )
        {
            player = brain.cachedPlayer = create( brain );
        }
        else
        {
            player.setState( access );
        }

        return player;
    }

    public static TurtlePlayer getWithPosition( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        TurtlePlayer turtlePlayer = get( turtle );
        turtlePlayer.setPosition( turtle, position, direction );
        return turtlePlayer;
    }

    private void setState( ITurtleAccess turtle )
    {
        if( currentScreenHandler != playerScreenHandler )
        {
            ComputerCraft.log.warn( "Turtle has open container ({})", currentScreenHandler );
            closeScreenHandler();
        }

        BlockPos position = turtle.getPosition();
        setPos( position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5 );

        setRotation( turtle.getDirection().asRotation(), 0 );

        getInventory().clear();
    }

    public void setPosition( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        double posX = position.getX() + 0.5;
        double posY = position.getY() + 0.5;
        double posZ = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if( turtle.getPosition().equals( position ) )
        {
            posX += 0.48 * direction.getOffsetX();
            posY += 0.48 * direction.getOffsetY();
            posZ += 0.48 * direction.getOffsetZ();
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            setRotation( direction.asRotation(), 0 );
        }
        else
        {
            setRotation( turtle.getDirection().asRotation(), DirectionUtil.toPitchAngle( direction ) );
        }

        setPos( posX, posY, posZ );
        prevX = posX;
        prevY = posY;
        prevZ = posZ;
        prevPitch = getPitch();
        prevYaw = getYaw();

        headYaw = getYaw();
        prevHeadYaw = headYaw;
    }

    public void loadInventory( @Nonnull ItemStack stack )
    {
        getInventory().clear();
        getInventory().selectedSlot = 0;
        getInventory().setStack( 0, stack );
    }

    public void loadInventory( @Nonnull ITurtleAccess turtle )
    {
        getInventory().clear();

        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().size();

        // Load up the fake inventory
        getInventory().selectedSlot = 0;
        for( int i = 0; i < slots; i++ )
        {
            getInventory().setStack( i, turtle.getItemHandler().getStack( (currentSlot + i) % slots ) );
        }
    }

    public void unloadInventory( ITurtleAccess turtle )
    {
        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().size();

        // Load up the fake inventory
        getInventory().selectedSlot = 0;
        for( int i = 0; i < slots; i++ )
        {
            turtle.getItemHandler().setStack( (currentSlot + i) % slots, getInventory().getStack( i ) );
        }

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        int totalSize = getInventory().size();
        for( int i = slots; i < totalSize; i++ )
        {
            ItemStack remainder = InventoryUtil.storeItems( getInventory().getStack( i ), turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getLevel(), dropPosition, dropDirection );
            }
        }

        getInventory().markDirty();
    }

    @Override
    public Vec3d getPos()
    {
        return new Vec3d( getX(), getY(), getZ() );
    }

    @Override
    public float getEyeHeight( @Nonnull EntityPose pose )
    {
        return 0;
    }

    @Override
    public float getActiveEyeHeight( @Nonnull EntityPose pose, @Nonnull EntityDimensions size )
    {
        return 0;
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openHandledScreen( @Nullable NamedScreenHandlerFactory prover )
    {
        return OptionalInt.empty();
    }

    @Override
    public void enterCombat()
    {
    }

    @Override
    public void endCombat()
    {
    }

    @Override
    public boolean startRiding( @Nonnull Entity entityIn, boolean force )
    {
        return false;
    }

    @Override
    public void stopRiding()
    {
    }

    @Override
    public void openEditSignScreen( @Nonnull SignBlockEntity signTile )
    {
    }

    @Override
    public void openHorseInventory( @Nonnull HorseBaseEntity horse, @Nonnull Inventory inventory )
    {
    }

    @Override
    public void useBook( @Nonnull ItemStack stack, @Nonnull Hand hand )
    {
    }

    @Override
    public void closeHandledScreen()
    {
    }

    @Override
    protected void onStatusEffectRemoved( @Nonnull StatusEffectInstance effect )
    {
    }
    //endregion
}
