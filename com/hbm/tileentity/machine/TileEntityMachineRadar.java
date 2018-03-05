package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.entity.missile.EntityMissileBaseAdvanced;
import com.hbm.interfaces.IConsumer;
import com.hbm.lib.ModDamageSource;
import com.hbm.main.MainRegistry;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.TERadarDestructorPacket;
import com.hbm.packet.TERadarPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TileEntityMachineRadar extends TileEntity implements IConsumer {

	public static List<EntityMissileBaseAdvanced> allMissiles = new ArrayList();
	public List<int[]> nearbyMissiles = new ArrayList();
	
	public static int range = 1000;

	public long power = 0;
	public static final int maxPower = 100000;

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		power = nbt.getLong("power");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setLong("power", power);
	}

	@Override
	public void updateEntity() {
		
		if(!worldObj.isRemote)
			nearbyMissiles.clear();
		
		if(power > 0) {

			if(!worldObj.isRemote) {
				allocateMissiles();
				sendMissileData();
			}
			
			power -= 500;
			if(power < 0)
				power = 0;
		}
		
		worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
		
		if(!worldObj.isRemote)
			PacketDispatcher.wrapper.sendToAll(new AuxElectricityPacket(xCoord, yCoord, zCoord, power));
	}
	
	private void allocateMissiles() {
		
		nearbyMissiles.clear();
		
		List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(xCoord + 0.5 - range, 0, zCoord + 0.5 - range, xCoord + 0.5 + range, 5000, zCoord + 0.5 + range));

		for(Entity e : list) {
			/*if(e instanceof EntityMissileBaseAdvanced) {
				EntityMissileBaseAdvanced mis = (EntityMissileBaseAdvanced)e;
				nearbyMissiles.add(new int[] { (int)mis.posX, (int)mis.posZ, mis.getMissileType() });
			}*/
			
			if(!(e instanceof EntityMissileBaseAdvanced) && e.width * e.width * e.height >= 0.5D && e.posY >= yCoord + 30) {
				nearbyMissiles.add(new int[] { (int)e.posX, (int)e.posZ, 5 });
			}
		}
		
		for(Entity e : allMissiles) {
			if(e instanceof EntityMissileBaseAdvanced) {
				if(e.posX < xCoord + range && e.posX > xCoord - range &&
						e.posZ < zCoord + range && e.posZ > zCoord - range) {
					EntityMissileBaseAdvanced mis = (EntityMissileBaseAdvanced)e;
					nearbyMissiles.add(new int[] { (int)mis.posX, (int)mis.posZ, mis.getMissileType() });
				}
			}
		}
	}
	
	public int getRedPower() {
		
		if(!nearbyMissiles.isEmpty()) {
			
			double maxRange = range * Math.sqrt(2D);
			
			int power = 0;
			
			for(int i = 0; i < nearbyMissiles.size(); i++) {
				
				int[] j = nearbyMissiles.get(i);
				double dist = Math.sqrt(Math.pow(j[0] - xCoord, 2) + Math.pow(j[1] - zCoord, 2));
				int p = 15 - (int)Math.floor(dist / maxRange * 15);
				
				if(p > power)
					power = p;
			}
			
			return power;
		}
		
		return 0;
	}
	
	private void sendMissileData() {
		
		PacketDispatcher.wrapper.sendToAll(new TERadarDestructorPacket(xCoord, yCoord, zCoord));
		
		for(int[] e : this.nearbyMissiles) {
			PacketDispatcher.wrapper.sendToAll(new TERadarPacket(xCoord, yCoord, zCoord, e[0], e[1], e[2]));
		}
	}
	
	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}
}