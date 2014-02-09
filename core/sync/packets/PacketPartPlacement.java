package appeng.core.sync.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.PartPlacement;

public class PacketPartPlacement extends AppEngPacket
{

	int x, y, z, face;

	// automatic.
	public PacketPartPlacement(ByteBuf stream) throws IOException {
		x = stream.readInt();
		y = stream.readInt();
		z = stream.readInt();
		face = stream.readByte();
	}

	@Override
	public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player)
	{
		EntityPlayerMP sender = (EntityPlayerMP) player;
		PartPlacement.place( sender.getHeldItem(), x, y, z, face, sender, sender.worldObj, PartPlacement.PlaceType.INTERACT_FIRST_PASS, 0 );
	}

	// api
	public PacketPartPlacement(int x, int y, int z, int face) throws IOException {

		ByteBuf data = Unpooled.buffer();

		data.writeInt( getPacketID() );
		data.writeInt( x );
		data.writeInt( y );
		data.writeInt( z );
		data.writeByte( face );

		configureWrite( data );
	}

}
