package coordinatedBoi;

import battlecode.common.*;
import scala.collection.Map;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    // Archon variables
    static boolean builtBuilder = false;
    static int startingMiners = 5;
    static int currMiners = 0;
    static int surroundingSoldiers = 2;
    static boolean builtLab = false;
    static int startingMIners2 = 10;

    // Comms array constants and variables
    static final int MAP_CENTER_START_INDEX = 0;
    static final int ARCHON_LOCATION_START_INDEX = 1;
    static int lead_farms_location_start_index = ARCHON_LOCATION_START_INDEX;

    // Other constants
    static final int TOO_MUCH_RUBBLE = 40;
    static final int COMMS_ARRAY_SIZE = 12;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        //System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!
            //System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case SOLDIER:    runSoldier(rc); break;
                    case LABORATORY: runLaboratory(rc); break;
                    case WATCHTOWER:
                    case BUILDER:    runBuilder(rc); break;
                    case SAGE:       break;
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Prints the entire contents of comms array
     * @param rc any RobotController that can access comms array
     */
    static void printCommsArray(RobotController rc) throws GameActionException {
        for (int i = 0; i < COMMS_ARRAY_SIZE; i++) {
            System.out.println(i + " " + rc.readSharedArray(i));
        }
    }

    /**
     * Gets the MapLocation stored in compressed form in the comms array at index.
     * If there is no MapLocation at index, returns (0,0). (this assumes that the center
     * of the map and our archons will never be at (0,0).
     * @param rc any RobotController that can access comms array
     * @param index the index in the comms array of the MapLocation
     * @return
     * @throws GameActionException
     */
    static MapLocation getLocationFromIndex(RobotController rc, int index) throws GameActionException {
        int coords = rc.readSharedArray(index);
        return new MapLocation(coords / 100, coords % 100);
    }

    /**
     * Compresses and writes loc to index in the comms array.
     * @param rc any RobotController that has access to comms array
     * @param index the index in the comms array to write loc
     * @param loc the MapLocation to write
     * @throws GameActionException
     */
    static void writeLocationToIndex(RobotController rc, int index, MapLocation loc) throws GameActionException {
        if (loc.y > 99) {
            System.out.println("\n\nASSUMPTION FAILED, MAP IS TOO TALL (Y > 99)\n\n");
        }
        rc.writeSharedArray(index, loc.x * 100 + loc.y);
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLaboratory(RobotController rc) throws GameActionException {
        if (rc.canTransmute()) {
            rc.transmute();
        }
    }

    static void runBuilder(RobotController rc) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if(!builtLab && rc.canBuildRobot(RobotType.LABORATORY, dir)) {
            rc.buildRobot(RobotType.LABORATORY, dir);
            builtLab = true;
        }
    }

    /**
     * Puts the location for rc in the comms array if it isn't already there.
     * @param rc the RobotController whose location should be put in comms
     * @throws GameActionException
     *
     */
    static void putArchonLocationInComms(RobotController rc) throws GameActionException {
        int index = ARCHON_LOCATION_START_INDEX;
        MapLocation currLoc = rc.getLocation();
        while (rc.readSharedArray(index) != 0) {
            if (getLocationFromIndex(rc, index).equals(currLoc)) {
                // This rc's location is already in the comms array
                return;
            }
            index++;
        }

        // Add this rc's location to the comms array at the next available index
        writeLocationToIndex(rc, index, currLoc);
        lead_farms_location_start_index = index + 1;
    }

    /**
     * Returns true if the archon rc should send the exploring soldier. This is if
     * the archon is the top right most archon and the soldier is not yet sent.
     * @param rc
     * @return
     * @throws GameActionException
     */
    static boolean isTopRightArchon(RobotController rc) throws GameActionException {
        MapLocation origin = new MapLocation(0, 0);
        int our_distance = rc.getLocation().distanceSquaredTo(origin);
        int index = ARCHON_LOCATION_START_INDEX;

        while (!getLocationFromIndex(rc, index).equals(origin)) {
            if (getLocationFromIndex(rc, index).distanceSquaredTo(origin) > our_distance) {
                return false;
            }
            index++;
        }

        return true;
    }

    static void runArchon(RobotController rc) throws GameActionException {
        // Put this archon's location in comms array if it isn't already
        putArchonLocationInComms(rc);

        // Send out 1 exploring soldier as early as possible after first wave of miners
        if (turnCount >= 2 && getLocationFromIndex(rc, MAP_CENTER_START_INDEX).equals(new MapLocation(0, 0))) {
            // If it is after the second turn (all archon locations are known), and there is no exploring soldier yet
            if (isTopRightArchon(rc) && rc.canBuildRobot(RobotType.SOLDIER, Direction.NORTHEAST)) {
                // If we are the top right archon, try to make the exploring soldier
                rc.buildRobot(RobotType.SOLDIER, Direction.NORTHEAST);
            } else {
                // If we are not the top right archon or are but couldn't make the soldier, wait till next turn
                return;
            }
        }

        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        // Need to have it more likely to make a soldier because the soldier costs more so
        // the miners will have more turns where they can be made

        if (!builtBuilder && currMiners == startingMiners) {
            boolean buildBuilder = rc.canBuildRobot(RobotType.BUILDER, dir);
            if (buildBuilder) {
                rc.buildRobot(RobotType.BUILDER, dir);
                builtBuilder = true;
            }
        }
        else if (!builtBuilder && currMiners < startingMiners) {
            boolean buildMiner = rc.canBuildRobot(RobotType.MINER, dir);
            if(buildMiner) {
                rc.buildRobot(RobotType.MINER, dir);
                currMiners++;
            }
        }
        else {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            int soldiers = 0;
            for (RobotInfo robot: nearbyRobots) {
                if(rc.getTeam() == robot.team && robot.getType() == RobotType.SOLDIER ) {
                    soldiers ++;
                }
            }
            boolean builtSoldier = true;
            if (soldiers < surroundingSoldiers) {
                builtSoldier = false;
                if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                    builtSoldier = true;
                }
            }
            if (currMiners < startingMIners2 && builtSoldier && rng.nextInt(6) >= 5) {
                // Let's try to build a miner.
                rc.setIndicatorString("Trying to build a miner");
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                    currMiners ++;
                }
            }
            else {
                if(rng.nextInt(100) >= 99) {
                    rc.setIndicatorString("Trying to build a miner");
                    if (rc.canBuildRobot(RobotType.MINER, dir)) {
                        rc.buildRobot(RobotType.MINER, dir);
                        currMiners ++;
                    }
                }
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        MapLocation locations[] = rc.senseNearbyLocationsWithLead(1);
        Direction dir;

        if (locations.length == 0){
            dir = directions[rng.nextInt(directions.length)];
        }
        else{
            //eventually have it go to random one in array
            int move;

            int index = rng.nextInt(locations.length);

            int dx = locations[index].x - me.x;
            int dy = locations[index].y - me.y;

            // we can switch to rc.getLocation().directionTo(locations[0].getLocation())
            if(dx < 0){
                if(dy < 0){
                    move = 5;
                }
                else if (dy > 0){
                    move = 7;
                }
                else{
                    move = 6;
                }
            }
            else if(dx > 0){
                if(dy < 0){
                    move = 3;
                }
                else if (dy > 0){
                    move = 1;
                }
                else{
                    move = 2;
                }
            }
            else{
                if(dy < 0){
                    move = 4;
                }
                else{
                    move = 0;
                }
            }

            dir = directions[move];
        }

        if (rc.canMove(dir)){
            rc.move(dir);
        }

//        Direction dir = directions[rng.nextInt(directions.length)];
//        if (rc.canMove(dir)) {
//            rc.move(dir);
//        }
    }

    /**
     * Checks if rc is the soldier tasked with exploring to the top right of the map.
     * Handles exploring and returns true if rc is the exploring bot, else returns false.
     *
     * @param rc
     * @throws GameActionException
     */
    static boolean handleExplorer(RobotController rc) throws GameActionException {
        if (rc.readSharedArray(MAP_CENTER_START_INDEX) == 0) {
            // If there is no exploring soldier set yet, this is the exploring soldier!
            rc.writeSharedArray(MAP_CENTER_START_INDEX, rc.getID());
        }

        if (rc.readSharedArray(MAP_CENTER_START_INDEX) == rc.getID()) {
            // This is the exploring soldier
            if (rc.canMove(Direction.NORTH)) {
                rc.move(Direction.NORTH);
            }
            if (rc.canMove(Direction.EAST)) {
                rc.move(Direction.EAST);
            }

            MapLocation east = rc.adjacentLocation(Direction.EAST);
            MapLocation north = rc.adjacentLocation(Direction.NORTH);
            if (!rc.onTheMap(east) && !rc.onTheMap(north)) {
                // We found the corner! Add the center of the map to the comms array
                // This overwrites the exploring soldier's id, so it behaves
                // like a regular soldier from this point onward
                MapLocation center = new MapLocation((east.x - 1) / 2, (north.y - 1) / 2);
                writeLocationToIndex(rc, MAP_CENTER_START_INDEX, center);
            }

            return true;
        }

        return false;
    }

    /**
     * Causes rc to attack the enemies at indices in the RobotInfo array enemies.
     *
     * @param enemies, the array of all enemies
     * @param indices, the indices of enemies to attack
     * @param rc, the robot controller
     * @throws GameActionException
     */
    static void attackEnemyList(RobotInfo[] enemies, List<Integer> indices, RobotController rc) throws GameActionException {
        for (Integer index : indices) {
            RobotInfo enemy = enemies[index];
            while (rc.canAttack(enemy.location) && enemy.getHealth() > 0) {
                rc.attack(enemy.location);
            }
        }
    }

    /**
     * Causes rc to move towards the enemies at indices in the RobotInfo array enemies.
     *
     * @param enemies, the array of all enemies
     * @param indices, the indices of enemies to move towards
     * @param rc, the robot controller
     * @throws GameActionException
     */
    static void moveTowardsEnemyList(RobotInfo[] enemies, List<Integer> indices, RobotController rc) throws GameActionException {
        for (Integer index : indices) {
            Direction enemyDir = rc.getLocation().directionTo(enemies[index].getLocation());
            while (rc.canMove(enemyDir)) {
                rc.move(enemyDir);
            }
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        if (handleExplorer(rc)) {
            return;
        }

        int actionRadius = rc.getType().actionRadiusSquared;
        int visionRadius = rc.getType().visionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadius, opponent);
        RobotInfo[] enemiesWeSee = rc.senseNearbyRobots(visionRadius, opponent);
        RobotInfo[] friendsWeSee = rc.senseNearbyRobots(visionRadius, rc.getTeam());

        // Get lists of all types of enemies in attacking range
        List<Integer> enemyArchons = new ArrayList<>();
        List<Integer> enemySages = new ArrayList<>();
        List<Integer> enemySoldiers = new ArrayList<>();
        List<Integer> enemyMiners = new ArrayList<>();
        for (int i = 0; i < enemies.length; i++) {
            RobotType type = enemies[i].getType();
            if (type == RobotType.ARCHON) {
                enemyArchons.add(i);
            } else if (type == RobotType.SAGE) {
                enemySages.add(i);
            } else if (type == RobotType.SOLDIER) {
                enemySoldiers.add(i);
            } else if (type == RobotType.MINER) {
                enemyMiners.add(i);
            }
        }

        // Attack all enemies we can in order of sage then soldier then miner
        attackEnemyList(enemies, enemyArchons, rc);
        attackEnemyList(enemies, enemySages, rc);
        attackEnemyList(enemies, enemySoldiers, rc);
        attackEnemyList(enemies, enemyMiners, rc);

        // Get lists of all types of enemies we see
        List<Integer> enemyArchonsWeSee = new ArrayList();
        List<Integer> enemyMinersWeSee = new ArrayList<>();
        for (int i = 0; i < enemiesWeSee.length; i++) {
            RobotInfo enemy = enemiesWeSee[i];
            if (enemy.getType() == RobotType.ARCHON) {
                enemyArchonsWeSee.add(i);
            } else if (enemy.getType() == RobotType.MINER) {
                enemyMinersWeSee.add(i);
            }
        }

        // Move towards archons we see, if none, move towards miners we see
        moveTowardsEnemyList(enemiesWeSee, enemyArchonsWeSee, rc);
        moveTowardsEnemyList(enemiesWeSee, enemyMinersWeSee, rc);

        // Default moves
        MapLocation center = getLocationFromIndex(rc, MAP_CENTER_START_INDEX);
        if (!center.equals(new MapLocation(0, 0))) {
            // We know where the center is
            Direction to_center = rc.getLocation().directionTo(center);
            boolean soldier_nearby = false;
            for (RobotInfo friend : friendsWeSee) {
                if (friend.getType() == RobotType.SOLDIER) {
                    soldier_nearby = true;
                }
            }
            if (soldier_nearby) {
                Direction newDir = to_center.rotateLeft().rotateLeft().rotateLeft();
                if (rc.canMove(newDir)) {
                    rc.move(newDir);
                }
            } else if (rc.canMove(to_center)) {
                rc.move(to_center);
            }
        }

        // If we still have moves left, we are probably stuck so we should move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
