package com.ojcoleman.ahni.evaluation;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.ojcoleman.ahni.util.Point;
import org.jgapcustomised.Chromosome;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Kevin on 30-May-17.
 */

public class SnakeFitnessFunction extends HyperNEATFitnessFunction {

    /**
     *
     * Max steps of stagnation without dying or increasing in size before terminating
     */
    private static final String MAX_STEP = "fitness.function.max_steps";

    /**
     *
     * Size of initial 2 dimensional grid, represented as int array.
     */
    private static final String GRID_SIZE = "fitness.function.grid_size";

    /**
     * Used to specify test type, if none stated it will use normal.
     *
     * Normal --> normal snake game, fitness is based on final snake length.
     * Survival --> fitness is based on time alive, snake cannot increase in length, but grid / snake gets increasingly bigger
     * Obstacles --> Normal game of snake, but with extra obstacles.
     */
    private static final String TEST_TYPE = "fitness.function.test_type";

    /**
     * Number of trials per substrate, Trials is sat to 1 if none specified
     */
    private static final String TRIAL_COUNT = "fitness.function.trial_count";


    /**
     * Initial length of the snake. The snake will atm always be place in the middle.
     */
    private static final String INI_SNAKE_LENGTH = "fitness.function.initial_snake_length";

    /**
     * States if there should be generated a video of the champion after a run
     */
    private static final String VIDEO_LOG = "fitness.function.video_log";

    //Props arguments
    private int maxSteps;
    private int[] gridSize;
    private String testType;
    private int trialCount;
    private int iniSnakeLength;
    private boolean videoLog;


    private double[][] gameGrid;
    private Random r = new Random();
    private ArrayList<int[]> bodyPositions;
    private int[] awardPosition;





    @Override
    public void init(Properties props) {
        super.init(props);

        maxSteps = props.getIntProperty(MAX_STEP, 100);
        gridSize = props.getIntArrayProperty(GRID_SIZE, new int[]{20,20});
        testType = props.getProperty(TEST_TYPE, "normal");
        trialCount = props.getIntProperty(TRIAL_COUNT, 20);
        iniSnakeLength = props.getIntProperty(INI_SNAKE_LENGTH, 5);
        videoLog = props.getBooleanProperty(VIDEO_LOG, false);

        bodyPositions = new ArrayList<>();
        gameGrid = new double[gridSize[0]][gridSize[1]];


    }


    @Override
    protected double evaluate(Chromosome genotype, Activator substrate, int evalThreadIndex) {
        bodyPositions = new ArrayList<>();
        int mY = gridSize[1] / 2;
        for(int i = 0; i < iniSnakeLength; i++){
            if( i == (iniSnakeLength - 1)){

                int[] headPosition = new int[] {i, mY};
                bodyPositions.add(headPosition);
            }

            else {
                bodyPositions.add(new int[]{i,mY});
            }
        }

        awardPosition = placeAward();
        int[] velocity = new int[]{1,0};
        int awardscollected = 0;
        int steps = 0;
        int totalsteps = 0;
        boolean alive = true;
        updateGridPositions();


        while(steps < maxSteps && alive ){
            if(awardscollected > 10) break;
            int[] hP = bodyPositions.get(bodyPositions.size() - 1);
            double[][] responseSeq = substrate.next(gameGrid);
            double[] responses = responseSeq[0];

            double left = responses[0];
            double forward = responses[1];
            double right = responses[2];

            int newdirection = (left > right && left > forward) ? 0 : (forward > right) ? 1 : 2;
            int velX = velocity[0];
            int velY = velocity[1];

            switch(newdirection){
                case 0: //left
                    velX = (velocity[0] == 1 || velocity[0] == -1) ? 0 : (velocity[1] == 1) ? -1 : 1;
                    velY = (velocity[1] == 1 || velocity[1] == -1) ? 0 : (velocity[0] == 1) ? 1 : -1;

                    break;
                case 1: //forward
                    velX = velocity[0];
                    velY = velocity[1];
                    break;

                case 2: //right
                    velX = (velocity[0] == 1 || velocity[0] == -1) ? 0 : (velocity[1] == 1) ? 1 : -1;
                    velY = (velocity[1] == 1 || velocity[1] == -1) ? 0 : (velocity[0] == 1) ? -1 : 1;
                    break;

            }

            velocity[0] = velX;
            velocity[1] = velY;

            int[] newHead = new int[]{hP[0] + velX, hP[1] + velY };

            bodyPositions.add(newHead);
            bodyPositions.remove(0);


            alive = isInsideGrid();

            if(alive) {

                steps++;
                if (atAward()) {
                    awardscollected++;
                    steps = 0;
                    awardPosition = placeAward();

                }

                updateGridPositions();


                hP = bodyPositions.get(bodyPositions.size() - 1);
                alive = (gameGrid[hP[0]][hP[1]] != 1);
                totalsteps++;
                if(totalsteps > 1000)
                    System.out.println("HEY");
            }
        }


        double perf = totalsteps + awardscollected * 100;
        genotype.setPerformanceValue(perf / 2000.0);
        return perf / 2000.0;


    }

    private void updateGridPositions() {
        gameGrid = new double[gridSize[0]][gridSize[1]];
        int[] hP = bodyPositions.get(bodyPositions.size() - 1);
        for(int[] b : bodyPositions){
            gameGrid[b[0]][b[1]] = 1;

        }
        gameGrid[awardPosition[0]][awardPosition[1]] = 3;

        gameGrid[hP[0]][hP[1]] = 2;


    }

    private boolean isInsideGrid(){
        int[] headPosition = bodyPositions.get(bodyPositions.size() - 1);
        if(headPosition[0] >= gridSize[0])
            return false;
        else if(headPosition[1] >= gridSize[1])
            return false;
        else
            return (headPosition[0] >= 0 && headPosition[1] >= 0);
    }

    private boolean atAward() {
        int[] headPosition = bodyPositions.get(bodyPositions.size() - 1);

        return(awardPosition[0] == headPosition[0] && awardPosition[1] == headPosition[1]);

    }


    private int[] placeAward(){

        int awardX = 0, awardY = 0;
        boolean freeSpace = false;

        while(!freeSpace){
            awardX = (int)((gridSize[0] - 1) * r.nextDouble());
            awardY = (int)((gridSize[1] - 1) * r.nextDouble());

            if(gameGrid[awardX][awardY] != 1 && gameGrid[awardX][awardY] != 2 )
                freeSpace = true;
        }

        return new int[]{awardX,awardY};


    }


    @Override
    public int[] getLayerDimensions(int layer, int totalLayerCount) {
        if (layer == 0) // Input layer.
            // 3 range sensors plus reward.
            return new int[] { gridSize[0], gridSize[1] };
        else if (layer == totalLayerCount - 1) { // Output layer.
            return new int[] { 3, 1 }; // Action to perform next (left, forward, right).
        }
        return null;
    }

    @Override
    public Point[] getNeuronPositions(int layer, int totalLayerCount) {
        // Coordinates are given in unit ranges and translated to whatever range is specified by the
        // experiment properties.
        Point[] positions = null;
        if (layer == 0) { // Input layer.
            positions = new Point[gridSize[0] * gridSize[1]];
            int counter = 0;
            for(int i = 0; i < gridSize[0]; i++){
                for(int j = 0; j < gridSize[1]; j++){
                    positions[counter++] = new Point(i , j , 0);
                }
            }

        } else if (layer == totalLayerCount - 1) { // Output layer.

            positions = new Point[3];
            // Action to perform next (left, forward, right).
            for (int i = 0; i < 3; i++) {
                positions[i] = new Point((double) i / 2, 0.5, 1);
            }

        }

        return positions;
    }

}
