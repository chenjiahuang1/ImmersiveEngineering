/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.wooden;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.IETileProviderBlock;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public class WindmillBlock extends IETileProviderBlock
{
	public WindmillBlock(String name)
	{
		super(name, Properties.create(Material.WOOD).hardnessAndResistance(2, 5),
				BlockItemIE.class, IEProperties.MULTIBLOCKSLAVE, IEProperties.FACING_HORIZONTAL);
		setNotNormalBlock();
	}

	@Nullable
	@Override
	public TileEntity createBasicTE(BlockState state)
	{
		return new WindmillTileEntity();
	}
}