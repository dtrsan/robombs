package robombs.game;

public class MapInfo implements Comparable<MapInfo>{

	private String name=null;
	private String pix=null;
	private String realName=null;
	private String set="classic";
	
	public MapInfo(String name, String realName) {
		this.name=name;
		String[] parts=realName.split(",");
		if (parts.length==2) {
			this.realName=parts[0];
			this.set=parts[1];
		} else {
			this.realName=realName;
		}
		
		int pos=name.lastIndexOf(".map");
		if (pos==-1) {
			System.err.println("The level name "+name+" is not allowed!");
		} else {
			pix=name.substring(0, pos)+".jpg";
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getSet() {
		return set;
	}
	
	public String getPicture() {
		return pix;
	}
	
	public String getRealName() {
		return realName;
	}
	
	public int compareTo(MapInfo mi) {
		return getName().compareTo(mi.getName());
	}
	
	public String toString() {
		return getName()+"/"+getRealName();
	}
}
