package sonar.calculator.mod.common.tileentity.machines;

import java.util.List;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.calculator.mod.api.machines.ProcessType;
import sonar.calculator.mod.api.nutrition.IHealthProcessor;
import sonar.calculator.mod.api.nutrition.IHealthStore;
import sonar.calculator.mod.common.recipes.machines.HealthProcessorRecipes;
import sonar.core.common.tileentity.TileEntitySidedInventory;
import sonar.core.inventory.SonarTileInventory;
import sonar.core.network.utils.ISyncTile;
import sonar.core.utils.helpers.FontHelper;
import sonar.core.utils.helpers.NBTHelper.SyncType;

public class TileEntityHealthProcessor extends TileEntitySidedInventory implements ISidedInventory, ISyncTile, IHealthProcessor {

	public int storedpoints, speed = 4;

	public TileEntityHealthProcessor() {
		super.input = new int[] { 0 };
		super.output = new int[] { 1 };
		super.inv = new SonarTileInventory(this, 2);
	}

	@Override
	public void update() {
		super.update();
		if (!this.worldObj.isRemote)
			loot(slots()[0]);

		charge(slots()[1]);
		this.markDirty();
	}

	public void charge(ItemStack stack) {
		if (!(stack == null) && this.storedpoints != 0) {
			if (stack.getItem() instanceof IHealthStore) {
				IHealthStore module = (IHealthStore) stack.getItem();
				int health = module.getHealthPoints(stack);
				int max = module.getMaxHealthPoints(stack);
				if (!(health >= max) || max == -1) {
					if (storedpoints >= speed) {
						if (max == -1 || max >= health + speed) {
							module.transferHealth(speed, stack, ProcessType.ADD);
							storedpoints = storedpoints - speed;
						} else if (max != -1) {
							module.transferHealth(max - health, stack, ProcessType.ADD);
							storedpoints = storedpoints - (max - health);
						}
					} else if (storedpoints <= speed) {
						if (max == -1 | max >= health + speed) {
							module.transferHealth(speed, stack, ProcessType.ADD);
							storedpoints = 0;
						} else if (max != -1) {
							module.transferHealth(max - health, stack, ProcessType.ADD);
							storedpoints = storedpoints - max - health;
						}
					}
				}
			}
		}

	}

	@Override
	public int getHealthPoints() {
		return storedpoints;
	}

	private void loot(ItemStack stack) {
		if (!(stack == null)) {
			if (isLoot(stack)) {
				int add = (Integer) HealthProcessorRecipes.instance().getOutput(stack);
				storedpoints = storedpoints + add;
				this.slots()[0].stackSize--;
				if (this.slots()[0].stackSize <= 0) {
					this.slots()[0] = null;
				}
			}
			if (stack.getItem() instanceof IHealthStore) {

				IHealthStore module = (IHealthStore) stack.getItem();
				int health = module.getHealthPoints(stack);
				if (health != 0) {
					if (health >= speed) {
						module.transferHealth(speed, stack, ProcessType.REMOVE);
						storedpoints = storedpoints + speed;
					}
					if (health <= speed) {
						module.transferHealth(health, stack, ProcessType.REMOVE);
						storedpoints = storedpoints + health;
					}
				}
			}
		}
	}

	private boolean isLoot(ItemStack stack) {
		if ((Integer) HealthProcessorRecipes.instance().getOutput(stack) > 0) {
			return true;
		}
		return false;
	}

	public void readData(NBTTagCompound nbt, SyncType type) {
		super.readData(nbt, type);
		this.storedpoints = nbt.getInteger("Food");

	}

	public void writeData(NBTTagCompound nbt, SyncType type) {
		super.writeData(nbt, type);
		nbt.setInteger("Food", this.storedpoints);

	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, EnumFacing direction){
		return this.isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, EnumFacing slots){
		if (slot == 1) {
			if (this.storedpoints == 0) {
				return true;
			}
			if (!(this.storedpoints == 0)) {
				return false;
			}
		}
		return true;
	}

	@SideOnly(Side.CLIENT)
	public List<String> getWailaInfo(List<String> currenttip) {
		currenttip.add(FontHelper.translate("points.health") + ": " + storedpoints);
		return currenttip;
	}
	    
}
