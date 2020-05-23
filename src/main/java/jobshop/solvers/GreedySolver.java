package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.Arrays;

public class GreedySolver implements Solver
{
    String priority;

    public GreedySolver(String priority)
    {
        this.priority = priority;
    }

    public Result solve(Instance instance, long deadline)
    {
        Task[] taskRealisable = new Task[instance.numJobs];
        ResourceOrder base = new ResourceOrder(instance);
        int[] jobs =  new int[instance.numJobs];
        int[] machines = new int[instance.numMachines];
        int[] nM = new int[instance.numMachines];
        int[] nJ = new int[instance.numJobs];

        for(int i = 0; i < instance.numJobs; i++)
            taskRealisable[i] = new Task(i, 0);
        Arrays.fill(nJ, -1);
        Arrays.fill(jobs, 0);
        Arrays.fill(machines, 0);

        for(int j = 0; j < (instance.numJobs * instance.numTasks); j++)
        {
            int min = Integer.MAX_VALUE;
            Task t = null;
            int index = 0, max = 0;
            if (priority.equals("spt") || priority.equals("lrpt"))
            {
                for(int i = 0; i < taskRealisable.length; i++)
                {
                    if (priority.equals("spt"))
                    {
                        if (taskRealisable[i] != null)
                        {
                            if (min > instance.duration(taskRealisable[i]))
                            {
                                min = instance.duration(taskRealisable[i]);
                                index = i;
                                t = taskRealisable[i];
                            }
                        }
                    }
                    else if (priority.equals("lrpt"))
                    {
                        int rtime = 0;
                        if (taskRealisable[i] != null)
                        {
                            for (int k = taskRealisable[i].task; k < instance.numTasks; k++)
                                rtime += instance.duration(taskRealisable[i].job, k);
                            if (max < rtime)
                            {
                                max = rtime;
                                index = i;
                                t = taskRealisable[i];
                            }
                        }
                    }
                }
            }
            else if (priority.equals("est_spt") || priority.equals("est_lrpt"))
            {
                int op = 0;
                int mini = Integer.MAX_VALUE;
                for(int i = 0; i < taskRealisable.length; i++)
                {
                    if (taskRealisable[i] != null)
                    {
                        int debut = Math.max(jobs[taskRealisable[i].job], machines[instance.machine(taskRealisable[i])]);
                        if (mini > debut)
                        {
                            mini = debut;
                            op = 1;
                            nJ[0] = i;
                        }
                        else if (mini == debut)
                        {
                            nJ[op] = i;
                            op++;
                        }
                    }
                }
                if (priority.equals("est_spt"))
                {
                    for(int i = 0; i < op; i++)
                    {
                        if (taskRealisable[nJ[i]] != null)
                        {
                            if (min > instance.duration(taskRealisable[nJ[i]]))
                            {
                                min = instance.duration(taskRealisable[nJ[i]]);
                                index = nJ[i];
                                t = taskRealisable[nJ[i]];
                            }
                        }
                    }
                }
                else if (priority.equals("est_lrpt"))
                {
                    for(int i = 0; i < op; i++)
                    {
                        if (taskRealisable[nJ[i]] != null)
                        {
                            int rtime = 0;
                            for (int k = taskRealisable[nJ[i]].task; k < instance.numTasks; k++)
                                rtime += instance.duration(taskRealisable[nJ[i]].job, k);
                            if (max < rtime)
                            {
                                max = rtime;
                                index = nJ[i];
                                t = taskRealisable[nJ[i]];
                            }
                        }
                    }
                }
            }
            if (t != null)
            {
                int machine = instance.machine(t);
                base.tasksByMachine[machine][nM[machine]] = t;
                nM[machine]++;
                if (t.task + 1 < instance.numTasks)
                    taskRealisable[index] = new Task(t.job,t.task + 1);
                else
                    taskRealisable[index] = null;
                machines[machine] += instance.duration(t);
                jobs[t.job] += instance.duration(t);
            }
        }
        return new Result(instance, base.toSchedule(), Result.ExitCause.Blocked);
    }
}
