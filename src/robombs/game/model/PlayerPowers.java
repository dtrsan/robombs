package robombs.game.model;

import robombs.game.*;
import robombs.game.util.*;

public class PlayerPowers {

	public final static int NOT_SICK=0;
	public final static int FASTER=1;
	public final static int SLOWER=2;
	public final static int ONE_BOMB_ONLY=3;
	public final static int WEAK_BOMBS=4;
	public final static int DROP_IMMEDIATELY=5;
	
	private String[] DESCS={"","moves faster","moves slower","has one bomb only","has weak bombs","drops bombs immediatly"};
	
	private int bombCount=1;
	private int firePower=20;
	private float water=100;
	private long waterTime=0;
	private boolean canKick=false;
	private Ticker sickness=new Ticker(20000);
	private int sick=NOT_SICK;
	
	public int getBombCount() {
		if (isSick()!=ONE_BOMB_ONLY) {
			return bombCount;
		}
		return 1;
	}
	
	public int getFirePower() {
		if (isSick()!=WEAK_BOMBS) {
			return firePower;
		}
		return 10;
	}
	
	public String getSicknessDescription() {
		return DESCS[sick];
	}
	
	public int getSickness() {
		return sick;
	}
	
	public void addToBombCount(int cnt) {
		if (bombCount<Globals.maxBombs) {
			bombCount+=cnt;
		}
	}
	
	public boolean canKick() {
		return canKick;
	}
	
	public void setKick(boolean can) {
		canKick=can;
	}
	
	public void addToFirePower(int power) {
		if (firePower<Globals.maxPower) {
			firePower+=power;
		}
	}
	
	public int isSick() {
		if (sickness.getTicks()>0) {
			sick=NOT_SICK;
		}
		return sick;
	}
	
	public int makeSick() {
		sickness.reset();
		float si=(float) Math.random();
		if (si<1) {
			sick=DROP_IMMEDIATELY;
		}
		if (si<0.85) {
			sick=WEAK_BOMBS;
		}
		if (si<0.75) {
			sick=ONE_BOMB_ONLY;
		}
		if (si<0.6) {
			sick=SLOWER;
		}
		if (si<0.4) {
			sick=FASTER;
		}
		return sick;
	}
	
	public void useWater(long ticks) {
		if (water==100) {
			waterTime=Ticker.getTime();
		}
		water-=ticks;
		if (water<0) {
			water=0;
		}
	}
	
	public void refillWater() {
		water=100;
		waterTime=0;
	}
	
	public void addWater(long ticks) {
		water+=(float) ticks/3f;
		if (water>100) {
			water=100;
		}
	}
	
	public int getWater() {
		if (water==100 && Ticker.getTime()-waterTime<2000) {
			return (int) water-1;
		}
		return (int) water;
	}
	
	public int getMaxWater() {
		return 100;
	}
}
