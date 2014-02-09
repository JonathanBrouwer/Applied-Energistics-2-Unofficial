package appeng.tile.grindstone;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IGrinderEntry;
import appeng.api.implementations.tiles.ICrankable;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.WorldCoord;
import appeng.me.storage.MEIInventoryWrapper;
import appeng.tile.AEBaseInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.WrapperInventoryRange;
import appeng.util.item.ItemList;

public class TileGrinder extends AEBaseInvTile implements ICrankable
{

	int points;

	final int inputs[] = new int[] { 0, 1, 2 };
	final int sides[] = new int[] { 0, 1, 2, 3, 4, 5 };
	AppEngInternalInventory inv = new AppEngInternalInventory( this, 7 );

	@Override
	public void setOrientation(ForgeDirection inForward, ForgeDirection inUp)
	{
		super.setOrientation( inForward, inUp );
		getBlockType().onNeighborBlockChange( worldObj, xCoord, yCoord, zCoord, Platform.air );
	}

	private void addItem(InventoryAdaptor sia, ItemStack output)
	{
		ItemStack notAdded = sia.addItems( output );
		if ( notAdded != null )
		{
			WorldCoord wc = new WorldCoord( xCoord, yCoord, zCoord );

			wc.add( getForward(), 1 );

			List<ItemStack> out = new ArrayList();
			out.add( notAdded );

			Platform.spawnDrops( worldObj, wc.x, wc.y, wc.z, out );
		}
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int j)
	{
		if ( AEApi.instance().registries().grinder().getRecipeForInput( itemstack ) == null )
			return false;

		return i >= 0 && i <= 2;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j)
	{
		return i >= 3 && i <= 5;
	}

	@Override
	public IInventory getInternalInventory()
	{
		return inv;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		return sides;
	}

	@Override
	public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added)
	{

	}

	@Override
	public boolean canTurn()
	{
		if ( Platform.isClient() )
			return false;

		if ( null == this.getStackInSlot( 6 ) ) // Add if there isn't one...
		{
			IMEInventory<IAEItemStack> input = new MEIInventoryWrapper( new WrapperInventoryRange( this, inputs, true ), null );
			for (IAEItemStack i : input.getAvailableItems( new ItemList() ))
			{
				IGrinderEntry r = AEApi.instance().registries().grinder().getRecipeForInput( i.getItemStack() );
				if ( r != null )
				{
					if ( i.getStackSize() >= r.getInput().stackSize )
					{
						i = i.copy();
						i.setStackSize( r.getInput().stackSize );
						IAEItemStack ais = input.extractItems( (IAEItemStack) i, Actionable.MODULATE, null );
						if ( ais != null )
							this.setInventorySlotContents( 6, ais.getItemStack() );
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public void applyTurn()
	{
		if ( Platform.isClient() )
			return;

		points++;

		ItemStack processing = this.getStackInSlot( 6 );
		IGrinderEntry r = AEApi.instance().registries().grinder().getRecipeForInput( processing );
		if ( r != null )
		{
			if ( r.getEnergyCost() > points )
				return;

			points = 0;
			InventoryAdaptor sia = InventoryAdaptor.getAdaptor( new WrapperInventoryRange( this, 3, 3, true ), ForgeDirection.EAST );

			addItem( sia, r.getOutput() );

			float chance = (Platform.getRandomInt() % 2000) / 2000.0f;
			if ( chance <= r.getOptionalChance() )
				addItem( sia, r.getOptionalOutput() );

			this.setInventorySlotContents( 6, null );
		}
	}

	@Override
	public boolean canCrankAttach(ForgeDirection directionToCrank)
	{
		return getUp().equals( directionToCrank );
	}

}
