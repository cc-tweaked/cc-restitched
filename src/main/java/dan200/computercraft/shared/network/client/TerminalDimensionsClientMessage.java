package dan200.computercraft.shared.network.client;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The terminal and portable computer server-side configured dimensions.
 */
public class TerminalDimensionsClientMessage implements NetworkMessage
{

    private final int computerTermWidth;
    private final int computerTermHeight;
    private final int pocketTermWidth;
    private final int pocketTermHeight;

    public TerminalDimensionsClientMessage()
    {
        this.computerTermWidth = ComputerCraft.computerTermWidth;
        this.computerTermHeight = ComputerCraft.computerTermHeight;
        this.pocketTermWidth = ComputerCraft.pocketTermWidth;
        this.pocketTermHeight = ComputerCraft.pocketTermHeight;
    }

    public TerminalDimensionsClientMessage( @Nonnull FriendlyByteBuf buf )
    {
        computerTermWidth = buf.readVarInt();
        computerTermHeight = buf.readVarInt();
        pocketTermWidth = buf.readVarInt();
        pocketTermHeight = buf.readVarInt();
    }

    @Override
    public void toBytes( FriendlyByteBuf buf )
    {
        buf.writeVarInt( computerTermWidth );
        buf.writeVarInt( computerTermHeight );
        buf.writeVarInt( pocketTermWidth );
        buf.writeVarInt( pocketTermHeight );
    }

    @Override
    public void handle( PacketContext context )
    {
        ComputerCraft.computerTermWidth = this.computerTermWidth;
        ComputerCraft.computerTermHeight = this.computerTermHeight;
        ComputerCraft.pocketTermWidth = this.pocketTermWidth;
        ComputerCraft.pocketTermHeight = this.pocketTermHeight;
    }

}