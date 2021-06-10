package learning;

//导入java基础库
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

//导入MATSim库
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.ParseException;

//import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.io.ParseException;
//import com.vividsolutions.jts.io.WKTReader;

public class CreateDemandUtils {
    //输入路径
    private static final String NETWORKFILE="input/network.xml";
    private static final String REGION="input/ZONA2008.shp";

    private static final String WORK="input/trabalho.txt";
    private static final int WORKQUANT=13863;
    private static final String STUDY = "input/estudo.txt";
    private static final int STUDYQUANT = 53;
    private static final String OTHER = "input/outros.txt";
    private static final int OTHERQUANT = 4866;
    private static final String CENSUS = "input/frota.txt";

    //输出目录
    private static final String PLANS_FILEOUTPUT = "output/plans. xml";

    private static double SCALEFACTOR=0.1;

    //创建scenario
    private Scenario scenario;
    private Map<String,Geometry> shapeMap;

    //创建构造器
    CreateDemandUtils(){
        this.scenario=ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
    }

    //主函数
    private void run() throws  Exception{
        //Assign shapefiles and txts to maps
        this.shapeMap=readShapeFile(REGION,"RA");
        String[][] workpoints=readFacilities(WORK,WORKQUANT);
        String[][] studypoints=readFacilities(STUDY,STUDYQUANT);
        String[][] otherpoints=readFacilities(OTHER,OTHERQUANT);

        int regions=39;
        String frota[][]=readCensus(CENSUS,regions);

        //迭代每个RA
        //TODO 寻找其定义方式
        for (int x=1; x<regions;x++){
            double frontaRa=Double.parseDouble(frota[x][1]);
            double commuters=0.55*frontaRa*SCALEFACTOR;
            double workOnly=commuters*0.52;
            double studyOnly=commuters*0.078;
            double studyAndWork=commuters*0.117;
            double noActivity=commuters*0.28;
            Geometry home=this.shapeMap.get(Integer.toString(x));

            //创建主业为工作的人
            for(int a=1;a<=workOnly;a++){
                String mode="car";
                //整理出RA地图内的居住点
                Coord homec=drawRandomPointFromGeometry(home);
                Random rndLine=new Random();
                //工作地点在文本文件中的行数
                int workLine=rndLine.nextInt(WORKQUANT-1)+1;
                //记录工作点的坐标
                double xCoord=Double.parseDouble(workpoints[workLine][1]);
                double yCoord=Double.parseDouble(workpoints[workLine][2]);
                //记录工作点的工作时间
                double wOpenTime=Double.parseDouble(workpoints[workLine][3]);
                double wCloseTime=Double.parseDouble(workpoints[workLine][4]);
                String wtype=workpoints[workLine][5];
                Coord workc=new Coord(xCoord,yCoord);

                //“其他”类活动的分类点
                int otherLine=rndLine.nextInt(OTHERQUANT-1)+1;
                //记录“其他”的活动坐标
                xCoord=Double.parseDouble(otherpoints[otherLine][1]);
                yCoord=Double.parseDouble(otherpoints[otherLine][2]);
                String oType=otherpoints[otherLine][5];
                Coord otherC=new Coord(xCoord,yCoord);

                //创建具有特定特征的人
                createWorkOnly(x,a,homec,workc,wOpenTime,wCloseTime,wtype,
                        otherC,oType,mode,"work");
            }

            //创建主业为学习的人
            for (int a=1; a<=studyOnly;a++){
                String mode="car";
                ///RA地图内住宅分类点
                Coord homec=drawRandomPointFromGeometry(home);
                Random rndLine=new Random();
                //学习文件中的排序
                int studyLine=rndLine.nextInt(STUDYQUANT-1)+1;
                //学习的点坐标
                Double xCoord=Double.parseDouble(studypoints[studyLine][1]);
                Double yCoord=Double.parseDouble(studypoints[studyLine][2]);
                Coord studyc=new Coord(xCoord,yCoord);
                //对其他活动点进行排序
                int otherLine=rndLine.nextInt(OTHERQUANT-1)+1;
                //点位坐标
                xCoord=Double.parseDouble(otherpoints[otherLine][1]);
                yCoord=Double.parseDouble(otherpoints[otherLine][2]);
                String otype=otherpoints[otherLine][5];
                Coord otherc
            }
        }
    }

    /**
     * @description 使用普查数据，读取表格并转换成矩阵的方法
      * @Param: null
     * @return
    */
    private String[][] readCensus(String filePath, int n){
        String matrix[][]=new String[n][2];
        try {
            Scanner input=new Scanner(new FileReader(filePath));

            for(int i=0; i<n;i++){
                for (int j=0;j<2;j++){
                    matrix[i][j]=input.next();
                }
            }
        }
        catch (FileNotFoundException e){
            //TODO Auto-generated catch block
            e.printStackTrace();
        }
        return matrix;
    }

    /**
     * @description 使用输入点数据, 读取文本文件（把活动转化成矩阵）
      * @Param: null
     * @return  
    */
    private String[][] readFacilities(String filePath, int n){
        String matrix[][]=new String[n][6];
        try {
            Scanner input=new Scanner(new FileReader(filePath));

            for(int i=0; i<n;i++){
                for (int j=0;j<6;j++){
                    matrix[i][j]=input.next();
                }
            }
        }
        catch (FileNotFoundException e){
            //TODO Auto-generated catch block
            e.printStackTrace();
        }
        return matrix;
    }

    /**
     * @description 创建只工作的人的方法
      * @Param: null
     * @return  
    */
    private void createWorkOnly(int regadm, int i, Coord coordHome, Coord coordWork,
                                Double wOpenTime, Double wCloseTime, String wType,
                                Coord coordOther, String oType, String mode, String type)
    {
        //该人的身份和他们的居住RA、他们的活动类型和一个整数
        Id<Person> personId=Id.createPersonId(regadm+type+i);
        Person person=scenario.getPopulation().getFactory().createPerson(personId);

        Random rnd=new Random();
        Double wOpenPeriod=wCloseTime-wOpenTime;

        //创建所有计划通用的活动
        Activity work=scenario.getPopulation().getFactory()
                .createActivityFromCoord("w"+wType,coordWork);
        if (wOpenPeriod<=10*60*60){
            work.setStartTime(wOpenTime);
            work.setEndTime(wCloseTime);
        }else if(wOpenPeriod>10*60*60 && wOpenPeriod<=16*60*60){
            int shift=rnd.nextInt(1);
            work.setStartTime(wOpenTime+(shift*wOpenPeriod/2));
            work.setEndTime(wOpenTime+(shift+1)*wOpenPeriod/2);
        }else {
            int shift=rnd.nextInt(2);
            work.setStartTime(wOpenTime+(shift*wOpenPeriod/3));
            work.setEndTime(wOpenTime);
        }

        //创建一天中的第一个活动：回家
        Activity home=scenario.getPopulation().getFactory()
                .createActivityFromCoord("home", coordHome);

        //region 创建 plan1=H-W-H
        //创建 plan1=H-W-H
        Plan plan1=scenario.getPopulation().getFactory().createPlan();
        Activity home1=scenario.getPopulation().getFactory()
                .createActivityFromCoord("home", coordHome);
        //将退出时间指定为下次活动前40分钟
        home1.setEndTime(work.getStartTime()-40*60);

        //在此计划内添加序列个体
        plan1.addActivity(home1);
        Leg hw=scenario.getPopulation().getFactory().createLeg(mode);
        plan1.addLeg(hw);
        plan1.addActivity(work);
        Leg wh=scenario.getPopulation().getFactory().createLeg(mode);
        plan1.addLeg(wh);
        plan1.addActivity(home);

        //将计划添加到此代理人中
        person.addPlan(plan1);
        //endregion

        //region 创建 plan2=H-O-W-H
        //创建 plan2=H-O-W-H
        Plan plan2=scenario.getPopulation().getFactory().createPlan();
        Activity other1=scenario.getPopulation().getFactory()
                .createActivityFromCoord("o"+oType,coordOther);

        //另一个的活动在工作时间前40分钟结束，最后60分钟结束
        other1.setEndTime(work.getStartTime()-40*60);
        other1.setStartTime(other1.getEndTime()-40*60);
        Activity home2=scenario.getPopulation().getFactory()
                .createActivityFromCoord("home",coordHome);
        home2.setEndTime(other1.getStartTime()-60*60);

        //在此计划中添加顺序活动
        plan2.addActivity(home2);
        Leg ho=scenario.getPopulation().getFactory().createLeg(mode);
        plan2.addLeg(ho);
        plan2.addActivity(other1);
        Leg ow=scenario.getPopulation().getFactory().createLeg(mode);
        plan2.addLeg(ow);
        plan2.addActivity(work);
        plan2.addLeg(wh);
        plan2.addActivity(home);

        //添加此代理的成员
        person.addPlan(plan2);
        //endregion

        //region 创建plan3=H-W-O-H
        //创建plan3=H-W-O-H
        Plan plan3=scenario.getPopulation().getFactory().createPlan();

        Activity home3=scenario.getPopulation().getFactory()
                .createActivityFromCoord("home",coordHome);
        //将退出时间指定为下次活动前40分钟
        home3.setEndTime(work.getStartTime()-40*60);

        ///其他类型的活动不是按照顺序结束，而是在工作活动结束两小时后结束
        Activity other2=scenario.getPopulation().getFactory()
                .createActivityFromCoord("o"+oType,coordOther);
        other2.setEndTime(work.getEndTime()+(2*60*60));

        //在此计划中添加顺序活动
        plan3.addActivity(home3);
        plan3.addLeg(hw);
        plan3.addActivity(work);
        Leg wo=scenario.getPopulation().getFactory().createLeg(mode);
        plan3.addLeg(wo);
        plan3.addActivity(other2);
        Leg oh=scenario.getPopulation().getFactory().createLeg(mode);
        plan3.addLeg(oh);
        plan3.addActivity(home);

        //正在将计划添加到此代理的内存中
        person.addPlan(plan3);
        //endregion

        //region 创建plan4 = H-W-O-W-H
        //创建plan4 = H-W-O-W-H
        Plan plan4=scenario.getPopulation().getFactory().createPlan();
        //将劳动活动分为两部分
        Activity work1=scenario.getPopulation().getFactory()
                .createActivityFromCoord("w"+wType,coordWork);
        Activity work2=scenario.getPopulation().getFactory()
                .createActivityFromCoord("w"+wType,coordWork);

        Double lunchTime;
        //工作时间取决于工作小时数，即机构的总工作时间，如果它工作很多小时，就会产生轮班制
        if (wOpenPeriod<=10*60*60){
            work1.setStartTime(wOpenTime);
            lunchTime=wOpenTime+(wOpenPeriod-2)/2;
            work1.setEndTime(lunchTime);
            work2.setStartTime(lunchTime+(2*60*60));
            work2.setEndTime((2*lunchTime)+(2*60*60)-wOpenTime);
        }else if(wOpenPeriod>10*60*60 && wOpenPeriod<=16*60*60){
            int shift=rnd.nextInt(1);
            work1.setStartTime(wOpenTime+(shift*(wOpenPeriod-1)/2));
            lunchTime=wOpenTime+(shift+1)*(wOpenPeriod-1)/2;
            work1.setEndTime(lunchTime);
            work2.setStartTime(lunchTime+(2*60*60));
            work2.setEndTime((2*lunchTime)+(2*60*60)-wOpenTime);
        }else{
            int shift=rnd.nextInt(2);
            work1.setStartTime(wOpenTime+(shift*(wOpenPeriod-1)/3));
            lunchTime=wOpenTime+(shift+1)*(wOpenPeriod-1)/3;
            work1.setEndTime(lunchTime);
            work2.setStartTime(lunchTime+(2*60*60));
            work2.setEndTime((2*lunchTime)+(2*60*60)-wOpenTime);
        }

        //在工作活动中分配其他活动
        Activity other3=scenario.getPopulation().getFactory()
                .createActivityFromCoord("o"+oType,coordOther);
        other3.setStartTime(lunchTime+(30*60));
        other3.setEndTime(lunchTime+(30*60)+60*60);

        //在此计划中按顺序添加活动
        plan4.addActivity(home1);
        plan4.addLeg(hw);
        plan4.addActivity(work1);
        plan4.addLeg(wo);
        plan4.addActivity(other3);
        plan4.addLeg(ow);
        plan4.addActivity(work2);
        plan4.addLeg(wh);
        plan4.addActivity(home);

        //正在将计划添加到此代理的内存中
        person.addPlan(plan4);
        //endregion

        //将每个计划的选中概率分配为每个代理的活动概率
        Double prob=rnd.nextDouble()*100;

        if (prob<=68.17){
            person.setSelectedPlan(plan1);
        }else if(prob>68.17 && prob<=78.36){
            person.setSelectedPlan(plan2);
        }else if(prob>78.36 && prob<=88.55){
            person.setSelectedPlan(plan3);
        }else{
            person.setSelectedPlan(plan4);
        }

        //将创建的代理人添加到字段
        scenario.getPopulation().addPerson(person);
    }


    /**
     * @description 几何图形中随机点的绘制方法
      * @Param: null
     * @return  
    */
    private Coord drawRandomPointFromGeometry( Geometry g){
        Random rnd=MatsimRandom.getLocalInstance();
        Point p;
        double x,y;
        do{
            x=g.getEnvelopeInternal().getMinX()+rnd.nextDouble()
                    *(g.getEnvelopeInternal().getMaxX()-g.getEnvelopeInternal()
                    .getMinX());
            y=g.getEnvelopeInternal().getMinY()+rnd.nextDouble()
                    *(g.getEnvelopeInternal().getMaxY()-g.getEnvelopeInternal()
                    .getMinY());
            p=MGC.xy2Point(x,y);
        }while(!g.contains(p));
        Coord coord=new Coord(p.getX(),p.getY());
        return coord;
    }

    /**
     * @description 读取shapefile文件
      * @Param: null
     * @return  
    */
    public Map<String, Geometry> readShapeFile(String fileName, String attrString){
        Map<String,Geometry> shapeMap=new HashMap<String, Geometry>();
        for (SimpleFeature ft:ShapeFileReader.getAllFeatures(fileName)){

            GeometryFactory geometryFactory=new GeometryFactory();
            WKTReader wktReader=new WKTReader(geometryFactory);
            Geometry geometry;

            try {
                geometry=wktReader.read((ft.getAttribute("the_geom")).toString());
                shapeMap.put(ft.getAttribute(attrString).toString(),geometry);
            }catch (ParseException e){
                //TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return shapeMap;
    }
}
