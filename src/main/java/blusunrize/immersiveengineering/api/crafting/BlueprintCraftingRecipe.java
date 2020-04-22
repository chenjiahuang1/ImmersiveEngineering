/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.crafting;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.common.items.IEItems.Misc;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.ListUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author BluSunrize - 21.07.2015
 * <br>
 * These recipes are accessible in the Engineers Workbench, with a Engineers Blueprint item.<br>
 * For every "category" registered, a blueprint item will be added automatically.
 */
public class BlueprintCraftingRecipe extends MultiblockRecipe
{
	public static float energyModifier = 1;
	public static float timeModifier = 1;

	public static ArrayList<String> blueprintCategories = new ArrayList<>();
	public static ArrayListMultimap<String, BlueprintCraftingRecipe> recipeList = ArrayListMultimap.create();
	public static HashMap<String, ItemStack> villagerPrices = new HashMap<>();

	public String blueprintCategory;
	public ItemStack output;
	public IngredientWithSize[] inputs;

	public BlueprintCraftingRecipe(String blueprintCategory, ItemStack output, IngredientWithSize[] inputs)
	{
		this.blueprintCategory = blueprintCategory;
		this.output = output;
		this.inputs = inputs;

		setInputListWithSizes(Lists.newArrayList(this.inputs));
		this.outputList = ListUtils.fromItem(this.output);

		//Time and energy values are for the automatic workbench
		this.totalProcessEnergy = (int)Math.floor(23040*energyModifier);
		this.totalProcessTime = (int)Math.floor(180*timeModifier);
	}

	public static ItemStack getTypedBlueprint(String type)
	{
		ItemStack stack = new ItemStack(Misc.blueprint);
		ItemNBTHelper.putString(stack, "blueprint", type);
		return stack;
	}

	public boolean matchesRecipe(NonNullList<ItemStack> query)
	{
		return getMaxCrafted(query) > 0;
	}

	public int getMaxCrafted(NonNullList<ItemStack> query)
	{
		HashMap<ItemStack, Integer> queryAmount = new HashMap<>();
		for(ItemStack q : query)
			if(!q.isEmpty())
			{
				boolean inc = false;
				for(ItemStack key : queryAmount.keySet())
					if(ItemHandlerHelper.canItemStacksStack(q, key))
					{
						queryAmount.put(key, queryAmount.get(key)+q.getCount());
						inc = true;
					}
				if(!inc)
					queryAmount.put(q, q.getCount());
			}

		OptionalInt maxCrafted = OptionalInt.empty();
		for(IngredientWithSize ingr : inputs)
		{
			int maxCraftedWithIngredient = 0;
			int req = ingr.getCount();
			Iterator<Entry<ItemStack, Integer>> queryIt = queryAmount.entrySet().iterator();
			while(queryIt.hasNext())
			{
				Entry<ItemStack, Integer> e = queryIt.next();
				ItemStack compStack = e.getKey();
				if(ingr.test(compStack))
				{
					int taken = e.getValue()/req;
					if(taken > 0)
					{
						e.setValue(e.getValue()-taken*req);
						if(e.getValue() <= 0)
							queryIt.remove();
						maxCraftedWithIngredient += taken;
					}
				}
			}
			if(maxCraftedWithIngredient <= 0)
				return 0;
			else if(maxCrafted.isPresent())
				maxCrafted = OptionalInt.of(Math.min(maxCrafted.getAsInt(), maxCraftedWithIngredient));
			else
				maxCrafted = OptionalInt.of(maxCraftedWithIngredient);
		}
		return maxCrafted.orElse(0);
	}

	public NonNullList<ItemStack> consumeInputs(NonNullList<ItemStack> query, int crafted)
	{
		List<IngredientWithSize> inputList = new ArrayList<>(inputs.length);
		for(IngredientWithSize i : inputs)
			if(i!=null)
				inputList.add(i);

		NonNullList<ItemStack> consumed = NonNullList.create();
		Iterator<IngredientWithSize> inputIt = inputList.iterator();
		while(inputIt.hasNext())
		{
			IngredientWithSize ingr = inputIt.next();
			int inputSize = ingr.getCount()*crafted;

			for(int i = 0; i < query.size(); i++)
			{
				ItemStack queryStack = query.get(i);
				if(!queryStack.isEmpty())
					if(ingr.test(queryStack))
					{
						int taken = Math.min(queryStack.getCount(), inputSize);
						consumed.add(ApiUtils.copyStackWithAmount(queryStack, taken));
						if(taken >= queryStack.getCount()&&queryStack.getItem().hasContainerItem(queryStack))
							query.set(i, queryStack.getItem().getContainerItem(queryStack));
						else
							queryStack.shrink(taken);
						inputSize -= taken;
						if(inputSize <= 0)
						{
							inputIt.remove();
							break;
						}
					}

			}
		}
		return consumed;
	}

	public static void addRecipe(String blueprintCategory, ItemStack output, IngredientWithSize... inputs)
	{
		recipeList.put(blueprintCategory, new BlueprintCraftingRecipe(blueprintCategory, output, inputs));
		if(!blueprintCategories.contains(blueprintCategory))
			blueprintCategories.add(blueprintCategory);
	}

	public static BlueprintCraftingRecipe[] findRecipes(String blueprintCategory)
	{
		if(recipeList.containsKey(blueprintCategory))
		{
			List<BlueprintCraftingRecipe> list = recipeList.get(blueprintCategory);
			return list.toArray(new BlueprintCraftingRecipe[list.size()]);
		}
		return new BlueprintCraftingRecipe[0];
	}

	/**
	 * registers a type of blueprint to be up for sale at the IE villager. Stacksize of the price will be slightly randomized (+- 2)
	 */
	public static void addVillagerTrade(String blueprintCategory, ItemStack villagerPrice)
	{
		villagerPrices.put(blueprintCategory, villagerPrice);
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}
}