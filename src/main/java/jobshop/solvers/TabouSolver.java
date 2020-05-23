package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.ArrayList;
import java.util.List;

public class TabouSolver implements Solver
{
    int maxIter, duree;

    public TabouSolver(int maxIter, int duree)
    {
        this.maxIter = maxIter;
        this.duree = duree;
    }


    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order)
        {
            Task t = order.tasksByMachine[this.machine][this.t1];
            order.tasksByMachine[this.machine][this.t1] = order.tasksByMachine[this.machine][this.t2];
            order.tasksByMachine[this.machine][this.t2] = t;
        }
    }


    @Override
    public Result solve(Instance instance, long deadline)
    {
        List<ResourceOrder> neighbors = new ArrayList<ResourceOrder>();
        Result result = (new GreedySolver("est_lrpt")).solve(instance, System.currentTimeMillis() + 1000);
        ResourceOrder base = new ResourceOrder(result.schedule);
        ResourceOrder bestRO = new ResourceOrder(result.schedule);
        ResourceOrder bestGlobal = new ResourceOrder(result.schedule);
        Swap bestSwap = null;
        long time = System.currentTimeMillis(); int iter = 0;
        int[][] tab = new int[instance.numTasks * instance.numJobs * 2][instance.numTasks * instance.numJobs * 2];
        while(time + 1000 > System.currentTimeMillis() && iter < maxIter)
        {
            iter++;
            bestRO = null;
            bestSwap = null;
            int ms = Integer.MAX_VALUE;
            for(Block block : blocksOfCriticalPath(base))
            {
                for(Swap swap : neighbors(block))
                {
                    ResourceOrder desc = base.copy();
                    swap.applyOn(desc);
                    if (desc.toSchedule() != null)
                    {
                        if(desc.toSchedule().makespan() < ms & iter >= tab[swap.machine * instance.numMachines + swap.t1][swap.machine * instance.numMachines + swap.t2])
                        {
                            bestRO = desc;
                            bestSwap = swap;
                            ms = desc.toSchedule().makespan();
                        }
                    }
                }
            }
            if (bestRO != null)
            {
                tab[bestSwap.machine * instance.numMachines + bestSwap.t1][bestSwap.machine * instance.numMachines + bestSwap.t2] = iter + duree;
                base = bestRO;
                if (base.toSchedule().makespan() < bestGlobal.toSchedule().makespan())
                {
                    bestGlobal = base;
                }
            }
        }
        return new Result(instance, base.toSchedule(), Result.ExitCause.Blocked);
    }


    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order)
    {
        List<Block> blocks = new ArrayList<Block>();
        for (int i = 0; i < order.instance.numMachines; i++)
        {
            int a = 0, b = 0;
            for (int j = 0; j < order.instance.numJobs; j++)
            {
                if (order.toSchedule().criticalPath().contains(order.tasksByMachine[i][j]))
                {
                    b = j;
                }
                else
                {
                    if ((b - a) > 0)
                        blocks.add(new Block(i, a, b));
                    a = j + 1;
                    b = a;
                }
            }
            if ((b - a) > 0)
                blocks.add(new Block(i, a, b));
        }
        return blocks;
    }


    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block)
    {
        List<Swap> possibleSwaps= new ArrayList<Swap>();
        if ((block.lastTask - block.firstTask) != 1)
        {
            possibleSwaps.add(new Swap(block.machine, block.firstTask,block.firstTask + 1));
            possibleSwaps.add(new Swap(block.machine, block.lastTask,block.lastTask - 1));
        }
        else
        {
            possibleSwaps.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }
        return possibleSwaps;
    }
}
