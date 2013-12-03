package LogProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.sound.sampled.LineListener;
import org.apache.log4j.*;
import org.omg.CORBA.PUBLIC_MEMBER;

import Util.ConfigManager;

public class LogParser {
	private FileInputStream log=null;
	private String log_input=null;
	static final Logger logger = Logger.getLogger("LogProcessor.LogParser");
	
	public LogParser(String loginput){
		try {
			log_input=loginput;
			log= new FileInputStream(new File(loginput));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void masterParse()
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(log));
		
		logger.info("Creating temp parsed master log file:tmp_master_log.txt");
		File parsed_master_log = new File("tmp_master_log.txt");
		if(!parsed_master_log.exists()) {
			try {
				parsed_master_log.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.toString());
			}
		} 
		BufferedWriter writer=null;
		try {
			writer = new BufferedWriter(new FileWriter(parsed_master_log, false));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.error(e1.toString());
		}
        //System.out.println("Reading File line by line using BufferedReader");

        String line;
        String[] tokens = null;
		try {
			line = reader.readLine();
			while(line != null){
				if(line.startsWith("submitted"))
				{
					tokens=line.split(" ");
					//System.out.println(line);
					//System.out.println(tokens[6]+tokens[8]+" "+tokens[10]+" "+tokens[12]+" "+tokens[14]);
					writer.write(tokens[6]+tokens[8]+" "+tokens[10]+" "+tokens[12]+" "+tokens[14]+" ");
					String result=workerParser(tokens[10]);
					String execution=executionParser(tokens[10]);
					writer.write(result+" "+execution);
					writer.newLine();
				}
	            line = reader.readLine();
	        }
			logger.info("Capturing merge phase provenance information...");
			writer.write("Merge Information");
			writer.newLine();
			String merge_info=mergeParse(ConfigManager.getProperty("merge_log"));
			writer.write(merge_info);
			writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		} finally {
            try {
                reader.close();
                writer.close();
                log.close();
            } catch (IOException ex) {
            	// TODO Auto-generated catch block
    			logger.error(ex.toString());
            }
          }
		
		System.out.println("WorkQueue Logs Parsing Complete!");
     }		
	
	public String workerParser(String taskName)
	{
		String result=null;
		
		ConfigManager config_Manager=new ConfigManager();
		
		String worker_log_dir=config_Manager.getProperty("worker_log");
		logger.info("Loading worker node log:"+worker_log_dir+"-Task:"+taskName);
		File dir = new File(worker_log_dir);
		
		if(dir.isDirectory())
		{
			for (File worker_log : dir.listFiles()) {
				BufferedReader reader=null;
				try {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(worker_log)));
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String line;
				
				String worker_name=worker_log.getName();
				worker_name=worker_name.replace(".log", "");
				
				try {
					line = reader.readLine();
					while(line != null){
						if(line.contains(taskName)&&line.contains("./slosh"))
						{
							//System.out.println(line);
							line=reader.readLine();
							String[] tokens=null;
							tokens=line.split(" ");
							//System.out.print(worker_name+" "+tokens[0]+" "+tokens[1]+" ");
							result=worker_name+" "+tokens[0]+" "+tokens[1]+" ";
							while(!line.contains("Task complete"))
								line=reader.readLine();
							tokens=line.split(" ");
							//System.out.println(tokens[0]+" "+tokens[1]);
							result=result+tokens[0]+" "+tokens[1];
						}
			            line = reader.readLine();
			        }    
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.error(e.toString());
				} finally {
		            try {
		                reader.close();
		            } catch (IOException ex) {
		            	// TODO Auto-generated catch block
		    			logger.error(ex.toString());
		            }
		          }
			}
		}
		else {
			logger.error("No worker log information.");
		}
		
		return result;
	}
	
	public String executionParser(String trkName)
	{
		FileInputStream log2=null;
		
		try {
			log2= new FileInputStream(new File(log_input));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		}
		
		String result=null;
		
		BufferedReader reader2 = new BufferedReader(new InputStreamReader(log2));
		String line=null;
        String[] tokens = null;
		try {
			line = reader2.readLine();
			while(line != null){
				if(line.startsWith("TaskID"))
					break;
				line=reader2.readLine();
			}
			line=reader2.readLine();
			while(!line.startsWith("Total"))
			{
				if(line.contains(trkName))
				{
					tokens=line.split("\\s+");
					result=tokens[2]+" "+tokens[3];
					break;
				}
				line=reader2.readLine();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		} finally {
            try {
                reader2.close();
                log2.close();
            } catch (IOException ex) {
            	// TODO Auto-generated catch block
    			logger.error(ex.toString());
            }
          }
		
		return result;
	}
	
	public String mergeParse(String merge_log)
	{
		logger.info("Loading Merge Phase log file:"+merge_log);
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(merge_log))));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			logger.error(e1.toString());
		}

		String line;
		String[] tokens = null;
		String output="";
		try {
			line = reader.readLine();
			while(line!=null)
			{
				if(line.startsWith("Envelopes"))
				{
					String env_info=null;
					while(!line.startsWith("Done with"))
					//System.out.println(0);
					{
						if(line.startsWith("MEOW:"))	
						{
							env_info=line.replace("MEOW:", "").trim();
						}
						line=reader.readLine();
					}
			
					tokens=line.split("/");
					output=output+tokens[tokens.length-1]+";"+env_info+"\n";
				}
				
				if(line.startsWith("start time"))
				{
					tokens=line.split(" ");
					output=output+"start:"+tokens[2]+"\n";
				}
				
				if(line.startsWith("end time"))
				{
					tokens=line.split(" ");
					output=output+"end:"+tokens[2]+"\n";
				}
				
				if(line.startsWith("execution time"))
				{
					tokens=line.split(" ");
					output=output+"execution:"+tokens[2]+"\n";
				}
					
				line=reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		} finally{
			try {
				reader.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.toString());
			}
		}
		return output;
	}
	
	public void middlewareParser()
	{
		String _middleware_log="tmp_middleware_log.txt";
		
		logger.info("Creating temp middleware log file:"+_middleware_log);
		File middleware_log = new File(_middleware_log);
		if(!middleware_log.exists()) {
			try {
				middleware_log.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.toString());
			}
		} 
		BufferedWriter writer=null;
		try {
			writer = new BufferedWriter(new FileWriter(middleware_log, false));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.error(e1.toString());
		}
		
		String worker_log_dir=ConfigManager.getProperty("worker_log");
		
		String output="";
		
		File dir = new File(worker_log_dir);
		
		if(dir.isDirectory())
		{
			for (File worker_log : dir.listFiles()) {
				BufferedReader reader=null;
				try {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(worker_log)));
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String line;
				String[] tokens;
				
				String worker_name=worker_log.getName();
				worker_name=worker_name.replace(".log", "");
				output=output+worker_name;
				try {
					line = reader.readLine();
					while(line != null){
						if(line.contains("debug: System:"))
						{
							tokens=line.split("System:");
							output=output+"\t"+tokens[1].trim();
						}
						
						if(line.contains("debug: work_queue_worker version"))
						{
							tokens=line.split("debug:");
							output=output+"\t"+tokens[1].trim();
						}
						
						if(line.contains("dns: finding my hostname"))
						{
							tokens=line.split("finding my hostname:");
							output=output+"\t"+tokens[1].trim();
						}
						
						if(line.contains("wq: working for master"))
						{
							tokens=line.split(" ");
							output=output+"\t"+tokens[0]+" "+tokens[1].trim();
						}
						line=reader.readLine();
					}
				}catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				output=output+"\n";
			}
			
			try {
				writer.write(output);
				writer.newLine();
				logger.info(output);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.toString());
			}finally
			{
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.error(e.toString());
				}
			}
			
		}
	}
}
