package KarmaAdaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.LoggingPermission;

import javax.security.auth.callback.ConfirmationCallback;

import org.jdom2.Element;

import KarmaClient.client;
import LogProcessor.LogParser;
import Util.ConfigManager;

import org.apache.log4j.*;
public class SLOSHRun {
	static final Logger logger = Logger.getLogger("KarmaAdaptor.SLOSHRun");
	public static String config_path=""; 
	
	public static void main(String[] args)
	{
		if(args.length==1)
		{
			config_path=args[0];
			System.out.println("Config.properties Path:"+config_path);
			File config=new File(config_path);
			if(!config.exists())
			{
				System.out.println("Exception: config.properties not exists.");
				return;
			}
		}
		else
		{
			System.out.println("Usage: KARMADaemon <config.properties path>");
			return;
		}
		
		String log4JPropertyFile = ConfigManager.getProperty("Log4j_Properties");
		PropertyConfigurator.configure(log4JPropertyFile);
		ConfigManager config_Manager=new ConfigManager();
		
		logger.info("SLOSH provenance framework is invoked.");
		logger.info("Looping through log central location...");
		logger.info("Loading middleware log file...");
		
		String master_log=config_Manager.getProperty("master_log");
		logger.info("Master node log file:"+master_log);
		
		LogParser log_parser=new LogParser(master_log);
		
		log_parser.masterParse();
		log_parser.middlewareParser();
	
		String parsedLog="tmp_master_log.txt";
		
		SLOSHAdaptor karma_adaptor=new SLOSHAdaptor(parsedLog);
		
		logger.info("\n"+"SLOSH KARMA Adaptor is starting...\n");
		
		String notification_folder=karma_adaptor.workflowInvoked();
		ArrayList<Element> outputenv_list=karma_adaptor.DistributionPhase();
		karma_adaptor.MergePhase(outputenv_list);

		logger.info("\n"+"SLOSH KARMA Adaptor finished.\n");
		
		logger.info("\n"+"Deleting temp files...");
		File master_file = new File("tmp_master_log.txt");
		 
		if(master_file.delete()){
			logger.info(master_file.getName() + " is deleted!");
		}else{
			logger.error("Temp Master log Delete operation is failed.");
		}
		
		File middleware_file = new File("tmp_middleware_log.txt");
		 
		if(middleware_file.delete()){
			logger.info(middleware_file.getName() + " is deleted!");
		}else{
			logger.error("Temp Middleware log Delete operation is failed.");
		}
		
		logger.info("\n"+"Karma client is sending provenance notifications...");
		client karma_client=new client(notification_folder);
		karma_client.sendNotifications();
		logger.info("\n"+"Karma client finished working.");
	}
	
}
