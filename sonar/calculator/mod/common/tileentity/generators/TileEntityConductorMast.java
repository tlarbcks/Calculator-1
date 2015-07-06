package sonar.calculator.mod.common.tileentity.generators;

import java.util.Random;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import sonar.calculator.mod.CalculatorConfig;
import sonar.calculator.mod.api.SyncData;
import sonar.calculator.mod.api.SyncType;
import sonar.calculator.mod.common.recipes.machines.ConductorMastRecipes;
import sonar.calculator.mod.common.tileentity.machines.TileEntityTransmitter;
import sonar.calculator.mod.common.tileentity.machines.TileEntityWeatherStation;
import sonar.core.common.tileentity.TileEntityInventorySender;
import sonar.core.utils.helpers.SonarHelper;
import cofh.api.energy.EnergyStorage;

public class TileEntityConductorMast extends TileEntityInventorySender implements ISidedInventory {

	public int cookTime, lightingTicks, lightTicks;
	public int furnaceSpeed = 50;
	public int lightningSpeed, strikes;
	public int random;
	public int maxTransfer = CalculatorConfig.conductorRF;
	public int lastStations;
	public Random rand = new Random();

	public TileEntityConductorMast() {
		super.storage = new EnergyStorage(5000000, 5000000);
		super.slots = new ItemStack[2];
		super.maxTransfer = 10000;
	}

	public ItemStack recipe(ItemStack stack) {
		return ConductorMastRecipes.smelting().getSmeltingResult(stack);
	}

	public int recipeEnergy(ItemStack stack) {
		return ConductorMastRecipes.smelting().getPowerUsage(stack);
	}

	public void onLoaded() {
		this.setWeatherStationAngles(true, worldObj, xCoord, yCoord, zCoord);
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (this.cookTime > 0) {
			this.cookTime++;
		}
		if (!this.worldObj.isRemote) {
			if (this.canCook()) {
				if (cookTime == 0) {
					this.cookTime++;
				}

				if (this.cookTime == furnaceSpeed) {
					this.cookTime = 0;
					this.cookItem();
				}

			} else {
				this.cookTime = 0;
			}
		}
		if (!this.worldObj.isRemote) {
			if (this.storage.getMaxEnergyStored() != this.storage.getEnergyStored() && this.storage.getEnergyStored() < storage.getMaxEnergyStored() && this.lightningSpeed == 0) {
				this.random = 0 + (int) (Math.random() * +9);
				this.lightningSpeed = rand.nextInt(1800 - 1500) + getNextTime();
			}
		}
		if (this.lightningSpeed > 0) {

			if (this.lightingTicks >= lightningSpeed) {
				this.lightingTicks = 0;
				this.lightningSpeed = 0;
				strikes = this.getStrikes();
				if (this.worldObj.isRemote) {
					int currentstrikes;
					if (strikes > 5) {
						currentstrikes = 5;
					} else {
						currentstrikes = strikes;
					}
					while (currentstrikes != 0) {
						worldObj.spawnEntityInWorld(new EntityLightningBolt(worldObj, xCoord, yCoord + 4, zCoord));
						currentstrikes--;
					}
				}
				this.lastStations = this.getStations();
				lightTicks++;
			} else {
				this.lightingTicks++;
			}

		}
		if (lightTicks > 0) {
			int add = (((CalculatorConfig.conductorRF / 200) + (this.lastStations * (CalculatorConfig.weatherstationRF / 200))) * strikes);
			if (lightTicks < 200) {
				if (this.storage.getEnergyStored() + add <= this.storage.getMaxEnergyStored()) {
					lightTicks++;
					storage.receiveEnergy(add, false);

				} else {
					lightTicks++;
					storage.setEnergyStored(this.storage.getMaxEnergyStored());
				}
			} else {
				if (this.storage.getEnergyStored() + add <= storage.getMaxEnergyStored()) {
					lightTicks = 0;
					storage.receiveEnergy(add, false);
				} else {
					lightTicks++;
					storage.setEnergyStored(this.storage.getMaxEnergyStored());
				}
			}
		}

		addEnergy();
		this.markDirty();

	}

	private void addEnergy() {

		TileEntity entity = SonarHelper.getAdjacentTileEntity(this, ForgeDirection.DOWN);

		if (SonarHelper.isEnergyHandlerFromSide(entity, ForgeDirection.DOWN.getOpposite())) {

			this.storage.modifyEnergyStored(-SonarHelper.pushEnergy(entity, ForgeDirection.DOWN.getOpposite(), this.storage.extractEnergy(10000, true), false));
		}
	}

	private void lightningStrike(World world, int x, int y, int z) {
		if (storage.getEnergyStored() < storage.getMaxEnergyStored()) {
			world.spawnEntityInWorld(new EntityLightningBolt(world, x, y, z));
		}

	}

	public boolean canCook() {
		if (this.storage.getEnergyStored() == 0) {
			return false;
		}

		if (slots[0] == null) {
			return false;
		}

		if (slots[1] != null) {
			return false;
		}

		if (cookTime >= furnaceSpeed) {
			return true;
		}

		ItemStack itemstack = recipe(slots[0]);
		if (itemstack == null) {
			return false;
		}

		int itemEnergy = recipeEnergy(slots[0]);
		if (cookTime == 0) {
			if (this.storage.getEnergyStored() < itemEnergy) {
				return false;
			}
		}

		return true;

	}

	private void cookItem() {

		ItemStack itemstack = recipe(slots[0]);
		int energy = recipeEnergy(slots[0]);
		this.storage.modifyEnergyStored(-energy);

		if (this.slots[1] == null) {
			this.slots[1] = new ItemStack(itemstack.getItem(), 1);

		} else if (this.slots[1].isItemEqual(itemstack)) {
			this.slots[1].stackSize++;
		}
		this.slots[0].stackSize--;

		if (this.slots[0].stackSize <= 0) {
			this.slots[0] = null;
		}

	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.cookTime = nbt.getInteger("CookTime");
		this.lightingTicks = nbt.getInteger("Ticks");
		this.lightTicks = nbt.getInteger("lightTicks");
		this.lightningSpeed = nbt.getInteger("lightSpeed");
		this.random = nbt.getInteger("rand");
		this.lastStations = nbt.getInteger("lastStations");
		this.strikes = nbt.getInteger("strikes");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("CookTime", this.cookTime);
		nbt.setInteger("Ticks", this.lightingTicks);
		nbt.setInteger("lightTicks", this.lightTicks);
		nbt.setInteger("lightSpeed", this.lightningSpeed);
		nbt.setInteger("rand", this.random);
		nbt.setInteger("lastStations", this.lastStations);
		nbt.setInteger("strikes", this.strikes);
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection direction) {
		if (direction == ForgeDirection.DOWN) {
			return true;
		}
		return false;
	}

	@Override
	public void readInfo(NBTTagCompound tag) {

		this.storage.setEnergyStored(tag.getInteger("energy"));
	}

	@Override
	public void writeInfo(NBTTagCompound tag) {
		tag.setInteger("energy", this.storage.getEnergyStored());
	}

	public static void setWeatherStationAngles(boolean packet, World world, int xCoord, int yCoord, int zCoord) {
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				if (world.getTileEntity(xCoord + x, yCoord, zCoord + z) != null && world.getTileEntity(xCoord + x, yCoord, zCoord + z) instanceof TileEntityWeatherStation) {
					TileEntityWeatherStation station = (TileEntityWeatherStation) world.getTileEntity(xCoord + x, yCoord, zCoord + z);
					station.setAngle();
					world.markBlockForUpdate(xCoord + x, yCoord, zCoord + z);

				}
			}
		}
	}

	public int getStations() {
		int stations = 0;
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				if (this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z) != null && this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z) instanceof TileEntityWeatherStation) {
					TileEntityWeatherStation station = (TileEntityWeatherStation) this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z);
					if (station.x == this.xCoord && station.z == this.zCoord) {
						stations++;
					}
				}
			}
		}
		return stations;
	}

	public int getStrikes() {
		int trans = this.getTransmitters();
		if (trans != 0) {
			if (trans > 9) {
				return trans / 4;
			} else {
				return 1;
			}
		}
		return 1;

	}

	public int getNextTime() {
		int trans = this.getTransmitters();
		if (trans != 0) {
			if (trans > 9) {
				return 250;
			} else {
				return 1500 - (135 * trans);
			}
		}
		return 1500;

	}

	public int getTransmitters() {
		int transmitter = 0;
		for (int x = -20; x <= 20; x++) {
			for (int z = -20; z <= 20; z++) {
				if (this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z) != null && this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z) instanceof TileEntityTransmitter) {
					TileEntityTransmitter station = (TileEntityTransmitter) this.worldObj.getTileEntity(xCoord + x, yCoord, zCoord + z);
					transmitter++;

				}
			}
		}
		return transmitter;
	}

	@Override
	public void onSync(Object data, int id) {
		super.onSync(data, id);
		switch (id) {
		case SyncType.COOK:
			this.cookTime = (Integer)data;
			break;
		case SyncType.SPECIAL1:
			this.lightingTicks = (Integer)data;
			break;
		case SyncType.SPECIAL2:
			this.lightTicks = (Integer)data;
			break;
		case SyncType.SPECIAL3:
			this.random = (Integer)data;
			break;
		case SyncType.SPECIAL4:
			this.lightningSpeed = (Integer)data;
			break;
		}
	}

	@Override
	public SyncData getSyncData(int id) {
		switch (id) {
		case SyncType.COOK:
			return new SyncData(true, cookTime);
		case SyncType.SPECIAL1:
			return new SyncData(true, lightingTicks);
		case SyncType.SPECIAL2:
			return new SyncData(true, lightTicks);
		case SyncType.SPECIAL3:
			return new SyncData(true, random);
		case SyncType.SPECIAL4:
			return new SyncData(true, lightningSpeed);
		}
		return super.getSyncData(id);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == 0 ? new int[] { 1 } : new int[] { 0 };
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		if (slot == 0) {
			if (stack != null && recipe(stack) != null) {
				return true;
			} else {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 1;
	}
}