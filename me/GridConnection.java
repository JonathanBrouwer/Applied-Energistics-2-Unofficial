package appeng.me;

import java.util.Arrays;
import java.util.EnumSet;

import net.minecraftforge.common.util.ForgeDirection;
import appeng.api.exceptions.FailedConnection;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.util.IReadOnlyCollection;
import appeng.me.pathfinding.IPathItem;
import appeng.util.Platform;
import appeng.util.ReadOnlyCollection;

public class GridConnection implements IGridConnection, IPathItem
{

	final static private MENetworkChannelsChanged event = new MENetworkChannelsChanged();

	private GridNode sideA;
	private ForgeDirection fromAtoB;
	private GridNode sideB;

	byte visitorIterationNumber = Byte.MIN_VALUE;

	public int channelData = 0;

	public GridConnection(IGridNode aNode, IGridNode bNode, ForgeDirection fromAtoB) throws FailedConnection {

		GridNode a = (GridNode) aNode;
		GridNode b = (GridNode) bNode;

		if ( Platform.securityCheck( a, b ) )
			throw new FailedConnection();

		if ( a == null || b == null )
			throw new GridException( "Connection Forged Between null enties." );

		if ( a.hasConnection( b ) || b.hasConnection( a ) )
			throw new GridException( "Connection already exists." );

		sideA = a;
		this.fromAtoB = fromAtoB;
		sideB = b;

		if ( b.myGrid == null )
		{
			b.setGrid( a.getInternalGrid() );
		}
		else
		{
			if ( a.myGrid == null )
			{
				GridPropagator gp = new GridPropagator( b.getInternalGrid() );
				a.beginVisition( gp );
			}
			else if ( b.myGrid == null )
			{
				GridPropagator gp = new GridPropagator( a.getInternalGrid() );
				b.beginVisition( gp );
			}
			else if ( a.myGrid.size() > b.myGrid.size() )
			{
				GridPropagator gp = new GridPropagator( a.getInternalGrid() );
				b.beginVisition( gp );
			}
			else
			{
				GridPropagator gp = new GridPropagator( b.getInternalGrid() );
				a.beginVisition( gp );
			}
		}

		// a connection was destroyed RE-PATH!!
		IPathingGrid p = sideA.getInternalGrid().getCache( IPathingGrid.class );
		p.repath();

		sideA.addConnection( this );
		sideB.addConnection( this );
	}

	@Override
	public void destroy()
	{
		// a connection was destroyed RE-PATH!!
		IPathingGrid p = sideA.getInternalGrid().getCache( IPathingGrid.class );
		p.repath();

		sideA.removeConnection( this );
		sideB.removeConnection( this );

		sideA.validateGrid();
		sideB.validateGrid();
	}

	@Override
	public IGridNode a()
	{
		return sideA;
	}

	@Override
	public ForgeDirection getDirection(IGridNode side)
	{
		if ( fromAtoB == ForgeDirection.UNKNOWN )
			return fromAtoB;

		if ( sideA == side )
			return fromAtoB;
		else
			return fromAtoB.getOpposite();
	}

	@Override
	public IGridNode b()
	{
		return sideB;
	}

	@Override
	public IGridNode getOtherSide(IGridNode gridNode)
	{
		if ( gridNode == sideA )
			return sideB;
		if ( gridNode == sideB )
			return sideA;

		throw new GridException( "Invalid Side of Connection" );
	}

	@Override
	public boolean hasDirection()
	{
		return fromAtoB != ForgeDirection.UNKNOWN;
	}

	@Override
	public IReadOnlyCollection<IPathItem> getPossibleOptions()
	{
		return new ReadOnlyCollection( Arrays.asList( new IPathItem[] { (IPathItem) a(), (IPathItem) b() } ) );
	}

	@Override
	public void incrementChannelCount(int usedChannels)
	{
		channelData += usedChannels;
	}

	@Override
	public boolean canSupportMoreChannels()
	{
		return getLastUsedChannels() < 32; // max, PERIOD.
	}

	@Override
	public int getUsedChannels()
	{
		return (channelData >> 8) & 0xff;
	}

	public int getLastUsedChannels()
	{
		return channelData & 0xff;
	}

	@Override
	public IPathItem getControllerRoute()
	{
		if ( sideA.getFlags().contains( GridFlags.CANNOT_CARRY ) )
			return null;
		return sideA;
	}

	@Override
	public void setControllerRoute(IPathItem fast, boolean zeroOut)
	{
		if ( zeroOut )
			channelData &= ~0xff;

		if ( sideB == fast )
		{
			GridNode tmp = sideA;
			sideA = sideB;
			sideB = tmp;
			fromAtoB = fromAtoB.getOpposite();
		}
	}

	@Override
	public void finalizeChannels()
	{
		if ( getUsedChannels() != getLastUsedChannels() )
		{
			channelData = (channelData & 0xff);
			channelData |= channelData << 8;

			if ( sideA.getInternalGrid() != null )
				sideA.getInternalGrid().postEventTo( sideA, event );

			if ( sideB.getInternalGrid() != null )
				sideB.getInternalGrid().postEventTo( sideB, event );
		}
	}

	@Override
	public EnumSet<GridFlags> getFlags()
	{
		return EnumSet.noneOf( GridFlags.class );
	}

}
