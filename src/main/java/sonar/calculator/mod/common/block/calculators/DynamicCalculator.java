package sonar.calculator.mod.common.block.calculators;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.calculator.mod.Calculator;
import sonar.calculator.mod.common.tileentity.misc.TileEntityCalculator;
import sonar.calculator.mod.common.tileentity.misc.TileEntityCalculator.Dynamic;
import sonar.calculator.mod.network.CalculatorGui;
import sonar.core.common.block.SonarMachineBlock;
import sonar.core.common.block.SonarMaterials;
import sonar.core.utils.BlockInteraction;
import sonar.core.utils.helpers.FontHelper;

public class DynamicCalculator extends SonarMachineBlock {
	public DynamicCalculator() {
		super(SonarMaterials.machine, true, true);
	}

	public boolean operateBlock(World world, BlockPos pos, EntityPlayer player, BlockInteraction interact) {
		player.openGui(Calculator.instance, CalculatorGui.DynamicCalculator, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@SideOnly(Side.CLIENT)
	public String localBlockName(String string) {
		if (string.equals("stable")) {
			return FontHelper.translate("tile.StableStone.name");
		}
		if (string.equals("glass")) {
			return FontHelper.translate("tile.StableGlass.name");
		} else {
			return FontHelper.translate("tile.Air.name");
		}
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntityCalculator.Dynamic();
	}

	@Override
	public boolean dropStandard(IBlockAccess world, BlockPos pos) {
		return true;
	}

}
