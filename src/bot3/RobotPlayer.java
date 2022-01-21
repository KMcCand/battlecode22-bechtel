package bot3;

import battlecode.common.*;

import java.util.PriorityQueue;
import java.util.Comparator;
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
    static final int NUM_SOLDIERS_FOR_VIOLENT_ENEMY = 5;
    static final int NUM_SOLDIERS_FOR_PEACEFUL_ENEMY = 3;

    // Comms Array Indices
    static final int ARCHON_LOCATION_START_INDEX = 0;
    static final int LEAD_FARM_START_INDEX = ARCHON_LOCATION_START_INDEX + 4;
    static final int COMMS_ARRAY_PRINT_UP_TO = 12;

    // Comms Array Int Values
    static final MapLocation NO_INFO = new MapLocation(0, 0);
    static final int OUR_ARCHON_IS_SAFE = 0;

    // Miner constants
    static final int MINIMUM_LEAD = 1;

    // Soldier constants
    static final int IDEAL_SOLDIER_MINER_DISTANCE_SQUARED = 8;
    static final int SHIELD_ARCHON_MAX_DISTANCE = 18;

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
        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;

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
        for (int i = 0; i < COMMS_ARRAY_PRINT_UP_TO; i++) {
            System.out.println(i + " " + rc.readSharedArray(i));
        }
    }

    /**
     * Gets the integer stored with the location at index in comms array.
     * @param rc any RobotController that can access comms array
     * @param index
     * @return
     * @throws GameActionException
     */
    static int getIntFromIndex(RobotController rc, int index) throws GameActionException {
        return rc.readSharedArray(index) / 10000;
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
        return new MapLocation((coords % 10000) / 100, coords % 100);
    }

    /**
     * Sets the communication int at index in comms array to info
     * @param rc
     * @param index
     * @param info
     */
    static void writeIntToIndex(RobotController rc, int index, int info) throws GameActionException {
        // Int value cannot be too big
        assert(info <= 5);
        rc.writeSharedArray(index, info * 10000 + rc.readSharedArray(index) % 10000);
    }

    /**
     * Compresses and writes loc to index in the comms array.
     * @param rc any RobotController that has access to comms array
     * @param index the index in the comms array to write loc
     * @param loc the MapLocation to write
     * @throws GameActionException
     */
    static void writeLocationToIndex(RobotController rc, int index, MapLocation loc) throws GameActionException {
        // Location coordinates cannot be too big
        assert(loc.x <= 99 && loc.y <= 99);
        rc.writeSharedArray(index, (rc.readSharedArray(index) / 10000) * 10000 + loc.x * 100 + loc.y);
    }

    static void writeLocationAndIntToIndex(RobotController rc, int index, MapLocation loc, int info) throws GameActionException {
        // Location and info cannot be too big
        assert(loc.x <= 99 && loc.y <= 99);
        assert(info <= 5);
        rc.writeSharedArray(index, info * 10000 + loc.x * 100 + loc.y);
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
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction dir = center.directionTo(rc.getLocation());
        if(!builtLab) {
            if (!rc.onTheMap(rc.adjacentLocation(dir))) {
                for (Direction buildLabDir: Direction.allDirections()) {
                    if (rc.canBuildRobot(RobotType.LABORATORY, buildLabDir)) {
                        rc.buildRobot(RobotType.LABORATORY, buildLabDir);
                        builtLab = true;
                    }
                }
            }
            while (rc.onTheMap(rc.adjacentLocation(dir))) {
                rc.move(dir);
            }
        }
        else {
            boolean canRepairLab = false;
            RobotInfo lab = null;
            RobotInfo[] friendsVisible = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
            for (RobotInfo friend : friendsVisible) {
                if (friend.getHealth() != friend.getType().health) {
                    if (friend.getType() == RobotType.LABORATORY && rc.canRepair(friend.getLocation())) {
                        System.out.println("HERE");
                        lab = friend;
                        canRepairLab = true;
                    }
                }
            }
            if (canRepairLab) {
                rc.repair(lab.getLocation());
            }
        }
//            else {
//                for (RobotInfo friend : friendsVisible) {
//                    if (friend.getHealth() != friend.getType().health) {
//                        if(friend.getType() == RobotType.LABORATORY && rc.canRepair(friend.getLocation())) {
//                            System.out.println("HERE");
//                            lab = friend;
//                            canRepairLab = true;
//                        }
////                    if (rc.canRepair(friend.getLocation())) {
////                        // Repair if possible
////                        rc.repair(friend.getLocation());
////                    }
//                    }
//                }
//                // If nobody to repair, run default move
//                Direction defaultDir = getDefaultDirection(rc);
//                if (rc.canMove(defaultDir)) {
//                    rc.move(defaultDir);
//                }
//            }
//        }
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
        writeLocationAndIntToIndex(rc, index, currLoc, OUR_ARCHON_IS_SAFE);
    }

    /**
     * Returns true if the Direction dir is towards an archon on our team, false otherwise.
     * @param rc
     * @param dir
     * @return
     * @throws GameActionException
     */
    static boolean directionIsTowardsFriendlyArchon(RobotController rc, Direction dir) throws GameActionException {
        for (int i = ARCHON_LOCATION_START_INDEX; i < LEAD_FARM_START_INDEX; i++) {
            MapLocation currArchon = getLocationFromIndex(rc, i);
            if (currArchon.equals(NO_INFO)) {
                // We reached the end of the archons in comms array
                break;
            }

            Direction toFriendlyArchon = rc.getLocation().directionTo(currArchon);
            if (dir.dx == toFriendlyArchon.dx && dir.dy == toFriendlyArchon.dy) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the Direction dir goes towards the center of the map in
     * either x or y or both, false otherwise.
     * @param rc
     * @param dir
     * @return
     * @throws GameActionException
     */
    static boolean directionIsTowardsMapCenter(RobotController rc, Direction dir) throws GameActionException {
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction toCenter = rc.getLocation().directionTo(center);
        if (dir.dx * toCenter.dx >= 0 && dir.dy * toCenter.dy >= 0) {
            // If the x and y component of dir go towards center, return true
            return true;
        }

        return false;
    }

    /**
     * Returns an array of Direction objects that represents all directions that are
     * towards the center of the map and not towards a friendly archon.
     * @param rc
     * @return an array of Directions for rc to build in
     * @throws GameActionException
     */
    static List<Direction> getArchonBuildDir(RobotController rc) throws GameActionException {
        List<Direction> allValidDir = new ArrayList<>();
        for (Direction dir : directions) {
            if (!directionIsTowardsFriendlyArchon(rc, dir) && directionIsTowardsMapCenter(rc, dir)) {
                allValidDir.add(dir);
            }
        }
        return allValidDir;
    }

    /**
     * Causes rc to produce numSoldiers soldiers as close as possible to loc.
     * @param rc, the RobotController of an archon
     * @throws GameActionException
     */
    static void makeSoldiersTowardsLocation(RobotController rc, MapLocation loc, int numSoldiers) throws GameActionException {
        Direction toLoc = rc.getLocation().directionTo(loc);
        if (rc.canBuildRobot(RobotType.SOLDIER, toLoc)) {
            rc.buildRobot(RobotType.SOLDIER, toLoc);
        }

        Direction toLocRight = rc.getLocation().directionTo(loc);
        for (int i = 0; i < (numSoldiers - 1) / 2; i++) {
            toLoc = toLoc.rotateLeft();
            if (rc.canBuildRobot(RobotType.SOLDIER, toLoc)) {
                rc.buildRobot(RobotType.SOLDIER, toLoc);
            }

            toLocRight = toLocRight.rotateRight();
            if (rc.canBuildRobot(RobotType.SOLDIER, toLocRight)) {
                rc.buildRobot(RobotType.SOLDIER, toLocRight);
            }
        }
    }

    /**
     * Checks if enemies are near rc. If they are, produces a number of soldiers to attack them
     * and returns true. If there are no enemies, returns false
     * @param rc
     * @throws GameActionException
     */
    static boolean defendIfEnemies(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        List<Direction> underAttackFrom = new ArrayList<>();
        boolean sawEnemy = false;
        for (RobotInfo enemy : enemies) {
            RobotType type = enemy.getType();
            if (type == RobotType.SOLDIER || type == RobotType.ARCHON || type == RobotType.SAGE || type == RobotType.WATCHTOWER) {
                sawEnemy = true;
                makeSoldiersTowardsLocation(rc, enemy.getLocation(), NUM_SOLDIERS_FOR_VIOLENT_ENEMY);
                underAttackFrom.add(myLoc.directionTo(enemy.getLocation()));
            } else if (type == RobotType.MINER || type == RobotType.BUILDER || type == RobotType.LABORATORY) {
                sawEnemy = true;
                makeSoldiersTowardsLocation(rc, enemy.getLocation(), NUM_SOLDIERS_FOR_PEACEFUL_ENEMY);
                underAttackFrom.add(myLoc.directionTo(enemy.getLocation()));
            }
        }

        if (sawEnemy) {
            // Set the communications array to say our archon is in danger from a certain direction
            int archonStatus;
            if (underAttackFrom.size() > 1) {
                // Under attack from multiple directions
                archonStatus = 5;
            } else {
                Direction dir = underAttackFrom.get(0);
                if (dir.equals(Direction.NORTH) || dir.equals(Direction.NORTHEAST)) {
                    archonStatus = 1;
                } else if (dir.equals(Direction.EAST) || dir.equals(Direction.SOUTHEAST)) {
                    archonStatus = 2;
                } else if (dir.equals(Direction.SOUTH) || dir.equals(Direction.SOUTHWEST)) {
                    archonStatus = 3;
                } else {
                    archonStatus = 4;
                }
            }
            writeIntToIndex(rc, getNearestArchonIndex(rc), archonStatus);

            // Make a miner to pick up the lead after we kill the enemy
            RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
            boolean haveMiner = false;
            for (RobotInfo friend : friends) {
                if (friend.getType() == RobotType.MINER) {
                    haveMiner = true;
                }
            }
            if (!haveMiner) {
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(RobotType.MINER, dir)) {
                        rc.buildRobot(RobotType.MINER, dir);
                        break;
                    }
                }
            }
        }

        return sawEnemy;
    }

    /**
     * Causes rc to repair all friendly units within range.
     * @param rc
     * @throws GameActionException
     */
    static void repairNearby(RobotController rc) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canRepair(rc.adjacentLocation(dir))) {
                rc.repair(rc.adjacentLocation(dir));
            }
        }
    }

    static void runArchon(RobotController rc) throws GameActionException {
        // Put this archon's location in comms array if it isn't already
        putArchonLocationInComms(rc);

        // If we see enemies, produce soldiers and save resources
        if (defendIfEnemies(rc)) {
            return;
        }

        // Repair anybody nearby
        repairNearby(rc);

        // Pick a direction to build in.
        List<Direction> allValidDir = getArchonBuildDir(rc);
        Direction dir = allValidDir.get(rng.nextInt(allValidDir.size()));

        if (!builtBuilder && currMiners == startingMiners && isFurthestArchonFromCenter(rc)) {
            MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            boolean buildBuilder = rc.canBuildRobot(RobotType.BUILDER, center.directionTo(rc.getLocation()));
            if (buildBuilder) {
                rc.buildRobot(RobotType.BUILDER, center.directionTo(rc.getLocation()));
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
     * If rc can see more than two friends of the same type, returns the direction away from the
     * center of these friends. Else, returns null.
     * @param rc
     * @return
     * @throws GameActionException
     */
    static Direction checkClumped(RobotController rc) throws GameActionException {
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        int clumpAllowedDistance = (center.x / 2) * (center.x / 2);
        if (rc.getLocation().distanceSquaredTo(center) > clumpAllowedDistance) {
            // If we are far from the center, no need to prevent clumping
            return null;
        }

        RobotInfo[] friendsVisible = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        List<RobotInfo> sameTypeFriends = new ArrayList<>();
        for (RobotInfo friend : friendsVisible) {
            if (friend.getType() == rc.getType()) {
                sameTypeFriends.add(friend);
            }
        }
        int numSameType = sameTypeFriends.size();
        if (numSameType > 2) {
            int centerX = 0;
            int centerY = 0;
            for (RobotInfo friend : sameTypeFriends) {
                MapLocation friendLoc = friend.getLocation();
                centerX += friendLoc.x;
                centerY += friendLoc.y;
            }
            MapLocation friendsCenter = new MapLocation(centerX / numSameType, centerY / numSameType);
            return friendsCenter.directionTo(rc.getLocation());
        }

        return null;
    }

    /**
     * Returns true if this is our team's furthest archon from the center, false otherwise.
     * Breaks ties by which archon has the lowest id (is earliest in the comms array and will
     * produce troops first so will make gold fastest).
     * @param rc
     * @return
     * @throws GameActionException
     */
    static boolean isFurthestArchonFromCenter(RobotController rc) throws GameActionException {
        MapLocation center = new MapLocation(rc.getMapWidth(), rc.getMapHeight());
        int myDistance = rc.getLocation().distanceSquaredTo(center);
        for (int i = ARCHON_LOCATION_START_INDEX; i < LEAD_FARM_START_INDEX; i++) {
            MapLocation currArchon = getLocationFromIndex(rc, i);
            if (currArchon.equals(NO_INFO)) {
                // We reached the end of the archons in comms array
                break;
            }

            if (currArchon.distanceSquaredTo(center) > myDistance) {
                // There is an archon further than us
                return false;
            } else if (currArchon.distanceSquaredTo(center) == myDistance) {
                // Either this is us, or there is another archon of equal distance. If this is a
                // different archon, we are not the closest as ties are broken by which is earlier
                // in the comms array.
                return currArchon.equals(rc.getLocation());
            }
        }

        return true;
    }

    /**
     * Returns the MapLocation of the nearest archon to rc.
     * @param rc
     * @return
     * @throws GameActionException
     */
    static MapLocation getNearestArchon(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapLocation nearestArchon = getLocationFromIndex(rc, ARCHON_LOCATION_START_INDEX);
        for (int i = ARCHON_LOCATION_START_INDEX; i < LEAD_FARM_START_INDEX; i++) {
            MapLocation currArchon = getLocationFromIndex(rc, i);
            if (currArchon.equals(NO_INFO)) {
                // We reached the end of the archons in comms array
                break;
            }
            if (myLoc.distanceSquaredTo(currArchon) < myLoc.distanceSquaredTo(nearestArchon)) {
                nearestArchon = currArchon;
            }
        }

        return nearestArchon;
    }

    /**
     * Returns the index in the communications array of the nearest archon to rc.
     * @param rc
     * @return
     * @throws GameActionException
     */
    static int getNearestArchonIndex(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int nearestArchonIndex = ARCHON_LOCATION_START_INDEX;
        for (int i = ARCHON_LOCATION_START_INDEX; i < LEAD_FARM_START_INDEX; i++) {
            MapLocation currArchon = getLocationFromIndex(rc, i);
            if (currArchon.equals(NO_INFO)) {
                // We reached the end of the archons in comms array
                break;
            }

            MapLocation nearestArchon = getLocationFromIndex(rc, nearestArchonIndex);
            if (myLoc.distanceSquaredTo(currArchon) < myLoc.distanceSquaredTo(nearestArchon)) {
                nearestArchonIndex = i;
            }
        }

        return nearestArchonIndex;
    }

    /**
     * Returns a good default direction for a miner or soldier. The default direction is
     * calculated as going away from the nearest archon, unless this goes away from the center,
     * then the default direction is towards the center.
     * @param rc
     * @return
     * @throws GameActionException
     */
    static Direction getDefaultDirection(RobotController rc) throws GameActionException {
        // Do not be too clumped
        Direction dir = checkClumped(rc);
        if (dir != null) {
            return dir;
        }

        // Find the nearest archon
        MapLocation myLoc = rc.getLocation();
        Direction awayFromNearestArchon = getNearestArchon(rc).directionTo(myLoc);
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

        // Return the direction to center if the direction away from the nearest archon goes towards the edge
        Direction toCenter = myLoc.directionTo(center);
        if (awayFromNearestArchon.dx * toCenter.dx >= 0 && awayFromNearestArchon.dy * toCenter.dy >= 0) {
            return awayFromNearestArchon;
        } else {
            return toCenter;
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        int visionRadius = rc.getType().visionRadiusSquared;

        // Mine any gold we can reach
        for (MapLocation loc : rc.senseNearbyLocationsWithGold(actionRadius)) {
            while (rc.canMineGold(loc)) {
                rc.mineGold(loc);
            }
        }

        // Mine any lead we can reach as long as we don't deplete it
        for (MapLocation loc : rc.senseNearbyLocationsWithLead(actionRadius)) {
            while (rc.canMineLead(loc) && rc.senseLead(loc) > MINIMUM_LEAD) {
                rc.mineLead(loc);
            }
        }

        // Go towards any gold we see
        for (MapLocation loc : rc.senseNearbyLocationsWithGold(actionRadius)) {
            Direction goldDir = rc.getLocation().directionTo(loc);
            while (rc.canMove(goldDir)) {
                rc.move(goldDir);
            }
            return;
        }

        // Get the direction towards the most lead we see
        MapLocation maxLeadLoc = rc.getLocation();
        for (MapLocation loc : rc.senseNearbyLocationsWithLead(visionRadius)) {
            if (rc.senseLead(loc) > MINIMUM_LEAD && rc.senseLead(loc) > rc.senseLead(maxLeadLoc)) {
                maxLeadLoc = loc;
            }
        }

        // Go towards the most lead
        Direction leadDir = rc.getLocation().directionTo(maxLeadLoc);
        if (rc.canMove(leadDir)) {
            while (rc.canMove(leadDir)) {
                rc.move(leadDir);
            }
            return;
        }

        // If we see no gold or lead, follow the default move
        Direction defaultDir = getDefaultDirection(rc);
        if (rc.canMove(defaultDir)) {
            rc.move(defaultDir);
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

    static PriorityQueue<RobotInfo> getAttackPriority(RobotController rc) {
        PriorityQueue<RobotInfo> attackPriority = new PriorityQueue<> (new Comparator<RobotInfo>() {
            public int compare(RobotInfo r1, RobotInfo r2) {
                // Prioritize above all finishing off an enemy
                if (r1.getHealth() < 3) {
                    return -1;
                } else if (r2.getHealth() < 3) {
                    return 1;
                }

                // Then prioritize attacking enemies in order
                if (r1.getType() == RobotType.SAGE) {
                    return -1;
                } else if (r2.getType() == RobotType.SAGE) {
                    return 1;
                } else if (r1.getType() == RobotType.WATCHTOWER) {
                    return -1;
                } else if (r2.getType() == RobotType.WATCHTOWER) {
                    return 1;
                } else if (r1.getType() == RobotType.SOLDIER) {
                    return -1;
                } else if (r2.getType() == RobotType.SOLDIER) {
                    return 1;
                } else if (r1.getType() == RobotType.ARCHON) {
                    return -1;
                } else if (r2.getType() == RobotType.ARCHON) {
                    return 1;
                } else if (r1.getType() == RobotType.LABORATORY) {
                    return -1;
                } else if (r2.getType() == RobotType.LABORATORY) {
                    return 1;
                } else if (r1.getType() == RobotType.MINER) {
                    return -1;
                } else if (r2.getType() == RobotType.MINER) {
                    return 1;
                } else if (r1.getType() == RobotType.BUILDER) {
                    return -1;
                } else if (r2.getType() == RobotType.BUILDER) {
                    return 1;
                }

                return 0;
            }
        });

        int actionRadius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadius, opponent);
        for (RobotInfo enemy : enemies) {
            attackPriority.add(enemy);
        }

        return attackPriority;
    }

    /**
     * An attempt at making soldiers shield against a specific direction. Cause them to have
     * too many moves, not enough attacks. Deprecated.
     * Returns the 'score' for a given move for a soldier shielding an archon under attack.
     * @param distAfter the distance to the archon after the move
     * @param currDist the distance to the archon before the move
     * @return
     */
    static int shieldingSoldierPriorities(int distAfter, int currDist) {
        int answer = 0;
//        if (currDist < SHIELD_ARCHON_MIN_DISTANCE && distAfter > currDist) {
//            // First priority is move away from archon so we can produce guys
//            answer += 105;
//        }
//        if (distAfter > SHIELD_ARCHON_MAX_DISTANCE) {
//            // Do not move out of SHIELD_ARCHON_MAX_DISTANCE
//            answer -= 100;
//        }
//        if (distAfter > currDist) {
//            // Within the min and max distances, prioritize moving away from the archon so
//            // other soldiers can come in
//            answer += 10;
//        }

        return answer;
    }

    /**
     * Causes the soldier at rc to defend the nearest archon tightly
     * @param rc
     * @return
     * @throws GameActionException
     */
    static boolean shieldArchon(RobotController rc) throws GameActionException {
        int nearestArchonIndex = getNearestArchonIndex(rc);
        MapLocation nearestArchon = getLocationFromIndex(rc, nearestArchonIndex);
        int nearestArchonDist = rc.getLocation().distanceSquaredTo(nearestArchon);
        int threatDirection = getIntFromIndex(rc, nearestArchonIndex);

        if (nearestArchonDist > SHIELD_ARCHON_MAX_DISTANCE || threatDirection == OUR_ARCHON_IS_SAFE) {
            // Too far away to defend or nearest archon is not under attack
            return false;
        }

        if (nearestArchonDist < SHIELD_ARCHON_MAX_DISTANCE - 5) {
            // Get pretty far from archon to make space for other soldiers
            Direction awayFromArchon = nearestArchon.directionTo(rc.getLocation());
            if (rc.canMove(awayFromArchon)) {
                rc.move(awayFromArchon);
            } else if (rc.canMove(awayFromArchon.rotateLeft())) {
                rc.move(awayFromArchon.rotateLeft());
            } else if (rc.canMove(awayFromArchon.rotateRight())) {
                rc.move(awayFromArchon.rotateRight());
            }
        }

        return true;

        // If we are close to an archon that is in trouble, we must stay and defend
//        PriorityQueue<Direction> movePriority = new PriorityQueue<> (new Comparator<Direction>() {
//            public int compare(Direction d1, Direction d2) {
//                int d1Dist = nearestArchon.distanceSquaredTo(rc.adjacentLocation(d1));
//                int d1Score = shieldingSoldierPriorities(d1Dist, nearestArchonDist);
//                int d2Dist = nearestArchon.distanceSquaredTo(rc.adjacentLocation(d2));
//                int d2Score = shieldingSoldierPriorities(d2Dist, nearestArchonDist);
//
//                if (d1Score < d2Score) {
//                    return -1;
//                } else {
//                    return 1;
//                }
//            }
//        });

//        for (Direction dir : directions) {
//            movePriority.add(dir);
//        }

//        while (rc.isActionReady() && !movePriority.isEmpty()) {
//            Direction currMove = movePriority.poll();
//            if (rc.canMove(currMove)) {
//                // Only move one move to save actions for defense
//                rc.move(currMove);
//                break;
//            }
//        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        int visionRadius = rc.getType().visionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemiesWeSee = rc.senseNearbyRobots(visionRadius, opponent);

        // Attack enemies in order according to getAttackPriority
        PriorityQueue<RobotInfo> attackPriority = getAttackPriority(rc);
        while (rc.isActionReady() && !attackPriority.isEmpty()) {
            RobotInfo currAttack = attackPriority.poll();
            while (rc.canAttack(currAttack.getLocation()) && currAttack.getHealth() > 0) {
                rc.attack(currAttack.getLocation());
            }
        }

        // Get lists of all types of enemies we see
        List<Integer> enemyArchonsWeSee = new ArrayList();
        List<Integer> enemyMinersWeSee = new ArrayList<>();
        for (int i = 0; i < enemiesWeSee.length; i++) {
            RobotInfo enemy = enemiesWeSee[i];
            if (enemy.getType() == RobotType.ARCHON) {
                enemyArchonsWeSee.add(i);
            } else if (enemy.getType() == RobotType.MINER) {
                enemyMinersWeSee.add(i);
            } else if (enemy.getType() == RobotType.SOLDIER) {
                // If we see a soldier, do not move. Need to save as many actions as
                // possible to win the 1 v 1
                return;
            }
        }

        // Move towards archons we see
        moveTowardsEnemyList(enemiesWeSee, enemyArchonsWeSee, rc);

        // If we are just produced at an archon that is in trouble, stay close and save actions
        if (shieldArchon(rc)) {
            return;
        }

        // Move towards enemy miners we see
        moveTowardsEnemyList(enemiesWeSee, enemyMinersWeSee, rc);

        // Move towards our miners
        RobotInfo[] friendsVisible = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        for (RobotInfo friend : friendsVisible) {
            if (friend.getType() == RobotType.MINER) {
                MapLocation myLoc = rc.getLocation();
                MapLocation minerLoc = friend.getLocation();
                Direction dir = myLoc.directionTo(minerLoc);
                if (myLoc.distanceSquaredTo(minerLoc) > IDEAL_SOLDIER_MINER_DISTANCE_SQUARED) {
                    // Move towards our miner if we are too far
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        return;
                    }
                } else if (myLoc.distanceSquaredTo(minerLoc) < IDEAL_SOLDIER_MINER_DISTANCE_SQUARED) {
                    // Move away from our miner if we are too close
                    if (rc.canMove(dir.opposite())) {
                        rc.move(dir.opposite());
                        return;
                    }
                } else {
                    // We are a good distance from our miner. Just save actions.
                    return;
                }
            }
        }

        // Default move
        Direction dir = getDefaultDirection(rc);
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else {
            dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }
}
