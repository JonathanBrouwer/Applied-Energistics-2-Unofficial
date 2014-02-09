package appeng.recipes.ores;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class OreDictionaryHandler
{

	public static final OreDictionaryHandler instance = new OreDictionaryHandler();

	private List<IOreListener> ol = new ArrayList();

	/**
	 * Just limit what items are sent to the final listeners, I got sick of strange items showing up...
	 * 
	 * @param name
	 * @return
	 */
	private boolean shouldCare(String name)
	{
		return true;
	}

	@SubscribeEvent
	public void onOreDictionaryRegister(OreDictionary.OreRegisterEvent event)
	{
		if ( shouldCare( event.Name ) )
		{
			for (IOreListener v : ol)
				v.oreRegistered( event.Name, event.Ore );
		}
	}

	/**
	 * Adds a new IOreListener and immediately notifies it of any previous ores, any ores added latter will be added at
	 * that point.
	 * 
	 * @param n
	 */
	public void observe(IOreListener n)
	{
		ol.add( n );

		// notify the listener of any ore already in existance.
		for (String name : OreDictionary.getOreNames())
		{
			if ( shouldCare( name ) )
			{
				for (ItemStack item : OreDictionary.getOres( name ))
				{
					n.oreRegistered( name, item );
				}
			}
		}
	}

}
