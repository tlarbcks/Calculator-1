package sonar.calculator.mod.common.item.modules;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.calculator.mod.Calculator;
import sonar.calculator.mod.api.machines.ProcessType;
import sonar.calculator.mod.api.nutrition.IHungerStore;
import sonar.calculator.mod.utils.helpers.NutritionHelper;
import sonar.core.common.item.SonarItem;
import sonar.core.utils.helpers.FontHelper;

public class HungerModule extends SonarItem implements IHungerStore {

	public HungerModule() {
		maxStackSize = 1;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		return NutritionHelper.chargeHunger(stack, world, player, "points");
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitx, float hity, float hitz) {
		return NutritionHelper.useHunger(stack, player, world, pos, side, "points");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {
		super.addInformation(stack, player, list, par4);

		if (stack.hasTagCompound()) {
			list.add(FontHelper.translate("points.hunger") + ": " + getHungerPoints(stack));
		}
	}

	@Override
	public void transferHunger(int transfer, ItemStack stack, ProcessType process) {
		if (!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound nbtData = stack.getTagCompound();
		if (nbtData == null) {
			stack.getTagCompound().setInteger("points", 0);
		}
		int points = stack.getTagCompound().getInteger("points");
		if (process == ProcessType.REMOVE) {
			nbtData.setInteger("points", points - transfer);
		} else if (process == ProcessType.ADD) {
			nbtData.setInteger("points", points + transfer);
		}
	}

	@Override
	public int getHungerPoints(ItemStack stack) {
		return NutritionHelper.getIntegerTag(stack, "points");
	}

	@Override
	public int getMaxHungerPoints(ItemStack stack) {
		return 1000;
	}

	@Override
	public void setHunger(ItemStack stack, int health) {
		if (!(health < 0) && health <= this.getMaxHungerPoints(stack)) {
			if (!stack.hasTagCompound())
				stack.setTagCompound(new NBTTagCompound());
			NBTTagCompound nbtData = stack.getTagCompound();
			if (nbtData == null) {
				stack.getTagCompound().setInteger("points", 0);
			}
			nbtData.setInteger("points", health);
		}
	}

}
