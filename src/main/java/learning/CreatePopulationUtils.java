package learning;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.io.PopulationWriter;

public class CreatePopulationUtils {

    public static Coord coordSW;//西南角节点坐标
    public static Coord coordNE;//东北角节点坐标

    /**
     * @description 设定坐标边界 coordSW, coordNE
      * @Param: null
     * @return
    */
    private static void setMaxMinCoord(Network network){
        
        for (Id<Node> nodeId : network.getNodes().keySet()){
            Coord nodeCoord=network.getNodes().get(nodeId).getCoord();
            if (coordSW==null || coordNE==null){
                coordSW=nodeCoord;
                coordNE=nodeCoord;
                continue;
            }

            coordSW = new Coord(
                    coordSW.getX()<nodeCoord.getX()?coordSW.getX():nodeCoord.getX(),
                    coordSW.getY()<nodeCoord.getY()?coordSW.getY():nodeCoord.getY()
            );
            coordNE = new Coord(
                    coordNE.getX()>nodeCoord.getX()?coordNE.getX():nodeCoord.getX(),
                    coordNE.getY()>nodeCoord.getY()?coordNE.getY():nodeCoord.getY()
            );
        }
    }

    /**
     * @description 获得随机坐标
      * @Param: null
     * @return  
    */
    private static Coord getRandomCoord(){
        double x,y;
        x=coordSW.getX()+(coordNE.getX()-coordSW.getX())*Math.random();
        y=coordSW.getY()+(coordNE.getY()-coordSW.getY())*Math.random();
        return new Coord(x,y);
    }
    
    /**
     * @description 创建代理
      * @Param: null
     * @return  
    */
    private static Population fillScenario(Scenario scenario, int count){
        Population population=scenario.getPopulation();
        
        for (int i=0;i<count;i++){
            Coord coord=getRandomCoord();
            Coord coordWork=getRandomCoord();
            createOnePerson(scenario,population,i,coord,coordWork);
        }
        return population;
    }

    /**
     * @description 创建单一代理
      * @Param: null
     * @return  
    */
    private  static void createOnePerson(Scenario scenario, Population population, int i, Coord coord, Coord coordwork){
        Person person=population.getFactory().createPerson(Id.createPersonId("p_"+i));
        Plan plan=population.getFactory().createPlan();

        Activity home=population.getFactory().createActivityFromCoord("home",coord);
        home.setEndTime(randomTime(8*60*60,60*60));
        plan.addActivity(home);

        Leg hinweg=population.getFactory().createLeg("car");
        plan.addLeg(hinweg);

        Activity work=population.getFactory().createActivityFromCoord("work",coordwork);
        work.setEndTime(randomTime(19*60*60,3*60*60));
        plan.addActivity(work);

        Leg rueckweg=population.getFactory().createLeg("car");
        plan.addLeg(rueckweg);

        Activity home2=population.getFactory().createActivityFromCoord("home",coord);
        plan.addActivity(home2);

        person.addPlan(plan);
        population.addPerson(person);
    }
    
    /**
     * @description 产生符合高斯分布的随机时间
      * @Param: null
     * @return  
    */
    private static int randomTime(int normalTime, int variance){
        Random rand=new Random();
        return (int)(variance*rand.nextGaussian()+normalTime);
    }

    public static void run(String roadPath, String planPath, int count){
        Config config=ConfigUtils.createConfig();
        Scenario scenario=ScenarioUtils.createScenario(config);

        //读取network.xml到内存中
        new MatsimNetworkReader(scenario.getNetwork()).readFile(roadPath);

        setMaxMinCoord(scenario.getNetwork());
        fillScenario(scenario, count);

        //plan.xml 文件位置
        new PopulationWriter(scenario.getPopulation()).write(planPath);
        System.out.println("Done writing file to"+planPath);
    }
}
