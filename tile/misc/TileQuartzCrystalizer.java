package appeng.tile.misc;

import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import appeng.api.util.AECableType;
import appeng.tile.grid.AENetworkPowerTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;

public class TileQuartzCrystalizer extends AENetworkPowerTile
{

	final int sides[] = new int[] { 0, 1 };
	AppEngInternalInventory inv = new AppEngInternalInventory( this, 2 );
	int processingTime = 0;

	@Override
	public AECableType getCableConnectionType(ForgeDirection dir)
	{
		return AECableType.COVERED;
	}

	public TileQuartzCrystalizer() {
		gridProxy.setValidSides( EnumSet.noneOf( ForgeDirection.class ) );
		internalMaxPower = 1500;
		gridProxy.setIdlePowerUsage( 0 );
	}

	@Override
	public void setOrientation(ForgeDirection inForward, ForgeDirection inUp)
	{
		super.setOrientation( inForward, inUp );
		gridProxy.setValidSides( EnumSet.of( getUp(), getUp().getOpposite() ) );
		setPowerSides( EnumSet.of( getUp(), getUp().getOpposite() ) );
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
	public int getInventoryStackLimit()
	{
		return 1;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		return i == 0;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j)
	{
		return i == 1;
	}

	@Override
	public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added)
	{

	}

	public void activate(EntityPlayer player)
	{
		// TODO Auto-generated method stub

	}

}
