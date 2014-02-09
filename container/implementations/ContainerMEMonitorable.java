package appeng.container.implementations;

import java.io.IOException;
import java.nio.BufferOverflowException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IMEChest;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.parts.IPart;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReciever;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.util.Platform;
import appeng.util.item.ItemList;

public class ContainerMEMonitorable extends AEBaseContainer implements IMEMonitorHandlerReciever<IAEItemStack>
{

	final IMEMonitor<IAEItemStack> monitor;
	final IItemList<IAEItemStack> items = new ItemList<IAEItemStack>();

	protected ContainerMEMonitorable(InventoryPlayer ip, IStorageMonitorable montiorable, boolean bindInventory) {
		super( ip, montiorable instanceof TileEntity ? (TileEntity) montiorable : null, montiorable instanceof IPart ? (IPart) montiorable : null );

		if ( Platform.isServer() )
		{
			monitor = montiorable.getItemInventory();
			if ( monitor != null )
			{
				monitor.addListener( this, null );

				cellInv = monitor;

				if ( montiorable instanceof IPortableCell )
					powerSrc = (IPortableCell) montiorable;
				else if ( montiorable instanceof IMEChest )
					powerSrc = (IMEChest) montiorable;
				else if ( montiorable instanceof IGridHost )
				{
					IGridNode node = ((IGridHost) montiorable).getGridNode( ForgeDirection.UNKNOWN );
					if ( node != null )
					{
						IGrid g = node.getGrid();
						if ( g != null )
							powerSrc = g.getCache( IEnergyGrid.class );
					}
				}
			}
			else
				isContainerValid = false;
		}
		else
			monitor = null;

		if ( bindInventory )
			bindPlayerInventory( ip, 0, 0 );
	}

	public ContainerMEMonitorable(InventoryPlayer ip, IStorageMonitorable montiorable) {
		this( ip, montiorable, true );
	}

	@Override
	public void detectAndSendChanges()
	{
		if ( Platform.isServer() )
		{
			if ( !items.isEmpty() )
			{
				try
				{
					IItemList<IAEItemStack> monitorCache = monitor.getStorageList();

					PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();

					for (IAEItemStack is : items)
					{
						IAEItemStack send = monitorCache.findPrecise( is );
						if ( send == null )
						{
							is.setStackSize( 0 );
							piu.appendItem( is );
						}
						else
							piu.appendItem( send );
					}

					if ( !piu.isEmpty() )
					{
						items.resetStatus();

						for (Object c : this.crafters)
						{
							if ( c instanceof EntityPlayer )
								NetworkHandler.instance.sendTo( piu, (EntityPlayerMP) c );
						}
					}
				}
				catch (IOException e)
				{
					AELog.error( e );
				}
			}

			super.detectAndSendChanges();
		}
	}

	@Override
	public void addCraftingToCrafters(ICrafting c)
	{
		super.addCraftingToCrafters( c );

		if ( Platform.isServer() && c instanceof EntityPlayer && monitor != null )
		{
			try
			{
				PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();
				IItemList<IAEItemStack> monitorCache = monitor.getStorageList();

				for (IAEItemStack send : monitorCache)
				{
					try
					{
						piu.appendItem( send );
					}
					catch (BufferOverflowException boe)
					{
						NetworkHandler.instance.sendTo( piu, (EntityPlayerMP) c );

						piu = new PacketMEInventoryUpdate();
						piu.appendItem( send );
					}
				}

				NetworkHandler.instance.sendTo( piu, (EntityPlayerMP) c );
			}
			catch (IOException e)
			{
				AELog.error( e );
			}

		}
	}

	@Override
	public void onContainerClosed(EntityPlayer player)
	{
		super.onContainerClosed( player );
		if ( monitor != null )
			monitor.removeListener( this );
	}

	@Override
	public void removeCraftingFromCrafters(ICrafting c)
	{
		super.removeCraftingFromCrafters( c );

		if ( this.crafters.isEmpty() && monitor != null )
			monitor.removeListener( this );
	}

	@Override
	public void postChange(IMEMonitor<IAEItemStack> monitor, IAEItemStack change, BaseActionSource source)
	{
		items.add( change );
	}

	@Override
	public boolean isValid(Object verificationToken)
	{
		return true;
	}

}
