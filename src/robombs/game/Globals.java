package robombs.game;

public class Globals {

	public final static int LIVES = 3;
	public final static int ROUND_COMPLETED_TIME = 6000;
	public final static String GAME_VERSION = "1.02";

	public static float sparkPower = 0.2f;
	public static int sparkNumber = 3;

	public static int maxBombs = 6;
	public static int maxPower = 50;

	public static boolean compiledObjects = true;
	public static boolean useVBO = true;
	public static boolean allDynamic = false;

	public static boolean skyBox = true;

	public static boolean enhancedGraphics = true;
	public static boolean useShaders = true;
	public static boolean optimizeShaders = true;
	public static boolean normalMapping = true;
	public static boolean reflections = false;

	public static boolean correctSleepTime = true;

	public static int frameLimit = 75;

	public static boolean useLWJGLTimer = false;

	public static int stepBacks = System.getProperty("norecoil") == null ? 15 : 0;

	public static float bulletSpeed = 2f;

	public static long invincibleTime = 3000;

	public static float minZoom = 0.175f;
	public static float maxZoom = 2.0f;

	public static boolean useShadowMesh = true;
	public static float shadowMeshOffset = 1f;// 0.125f;
	public static boolean renderWallShadows = true;
	public static boolean shadowCulling = false;

	public static boolean useBombingGrid = true;
	public static float bombingGridWidth = 6;

	public static boolean activeServer = true;
	public static boolean verbose = true; // Not used ATM

	// Will be adjusted at runtime...
	public static boolean activeTransfer = true;
	public static boolean activeTransferForBots = true;

	// Everything above (i.e. below this value) is considered as invisible, i.e.
	// "not really there".
	// This value shouldn't be modified without a good reason!
	public static float skyLimit = -1000;

	public static int botsCanPassOwnBombsForMS = 50;

	public static boolean showCollisionMesh = false;

	public static boolean mute = false;

	// For Talkback
	public static String graphicsAdapter = "unknown";
	public static String shadowMode = "unknown";

	public static void setEnhancedGraphics(boolean setit) {
		if (setit) {
			enhancedGraphics = true;
			reflections = false;
			compiledObjects = true;
			useVBO = true;
		} else {
			enhancedGraphics = false;
			reflections = false;
			compiledObjects = false;
			useVBO = false;
		}
	}

}
