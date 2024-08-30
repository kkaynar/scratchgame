import org.json.*;
import java.util.*;
import java.math.*;
import java.io.*;

public class ScratchGame{
    	private static String[][] generateMatrix(JSONObject configObj){ 	
		String[][] matrix = new String[configObj.getInt("rows")][configObj.getInt("columns")];
		JSONArray gsymbols = configObj.getJSONObject("probabilities").getJSONArray("standard_symbols");
		JSONObject bsymbols = configObj.getJSONObject("probabilities").getJSONObject("bonus_symbols").getJSONObject("symbols"); 	
		Random rng = new Random();
		boolean bonusok = false;
		for(int i = 0; i < gsymbols.length(); i++){
			int sumA = gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("A");
			int sumB = sumA + gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("B");
			int sumC = sumB + gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("C");
			int sumD = sumC + gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("D");
			int sumE = sumD + gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("E");
			int sumF = sumE + gsymbols.getJSONObject(i).getJSONObject("symbols").getInt("F");
			int sum10x = sumF + bsymbols.getInt("10x");
			int sum5x = sum10x + bsymbols.getInt("5x");
			int sump1000 = sum5x + bsymbols.getInt("+1000");
			int sump500 = sump1000 + bsymbols.getInt("+500");
			int sumMISS = sump500 + bsymbols.getInt("MISS");
		
			double rand = rng.nextDouble() * (bonusok ? sumF : sumMISS); // I allowed only one bonus symbol
			String pref = null;
			if (rand < sumA) 
  				pref = "A";
			else if (rand < sumB)
  				pref = "B";
			else if (rand < sumC)
  				pref = "C";
			else if (rand < sumD)
  				pref = "D";
			else if (rand < sumE)
  				pref = "E";
			else if (rand < sumF)
  				pref = "F";
			else if (rand < sum10x){
  				pref = "10x";
				bonusok = true;
			}	
			else if (rand < sum5x){
  				pref = "5x";
				bonusok = true;
			}
			else if (rand < sump1000){
  				pref = "+1000";
				bonusok = true;
			}
			else if (rand < sump500){
  				pref = "+500";
				bonusok = true;
			}
			else{
				pref = "MISS";
				bonusok = true;
			}		
			matrix[gsymbols.getJSONObject(i).getInt("column")][gsymbols.getJSONObject(i).getInt("row")] = pref; 
		}
		
		/* You can test from here!
		matrix[0][0] = "A";
		matrix[0][1] = "B";
		matrix[0][2] = "C";
		matrix[1][0] = "E";
		matrix[1][1] = "B";
		matrix[1][2] = "5x";
		matrix[2][0] = "F";
		matrix[2][1] = "D";
		matrix[2][2] = "C";*/

		return matrix;
	}

	public static void main(String[] args) {
		if(args.length != 4 || !args[0].equals("--config") || !args[2].equals("--betting-amount")){
			System.out.println("Wrong number of parameters to run the program");
			return;
		}

		String betamount = args[3];
		BigDecimal dbetamount = null;
		try{
			dbetamount = new BigDecimal(betamount);		
		}
		catch(NumberFormatException exp){
			System.out.println("Wrong betting amount parameter to run the program");
			return;		
		}

		String configfile = args[1];
		if(configfile.equals("")){
			System.out.println("Wrong config file parameter to run the program");
			return;		
		}
		String configstring = null;
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader (configfile));
    			String line = null;
    			StringBuilder stringBuilder = new StringBuilder();
    			String ls = System.getProperty("line.separator");
        		while((line = reader.readLine()) != null) {
            			stringBuilder.append(line);
            			stringBuilder.append(ls);
        		}
			configstring = stringBuilder.toString();
		}
		catch(IOException exp){
			System.out.println("Config file read problem!");
			return;
		}
		finally {
			try{
				if(reader != null)
        				reader.close();
			}
			catch(IOException exp){
				System.out.println("Config file read problem!");
				return;
			}
    		}
	
		JSONObject configObj = new JSONObject(configstring);
		
		String[][] matrix = generateMatrix(configObj);
		//System.out.println(configObj.getInt("rows"));
		//System.out.println(configObj.getInt("columns"));
		JSONObject resultjson = calculateResult(matrix, configObj, dbetamount);	

		System.out.println(resultjson.toString(2));
    	}

	private static JSONObject calculateResult(String[][] matrix, JSONObject configObj, BigDecimal dbetamount){ 
			JSONObject resultjson = new JSONObject();
			JSONArray jsmatrix = new JSONArray();
			for(int r = 0; r < configObj.getInt("rows"); r++){
				JSONArray jsrow = new JSONArray();
				jsmatrix.put(jsrow);
				for(int c = 0; c < configObj.getInt("columns"); c++)
					jsrow.put(matrix[r][c]);
			}
			//System.out.println(jsmatrix);
			resultjson.put("matrix", jsmatrix);
			JSONObject appliedwins = new JSONObject();
			JSONObject wincons = configObj.getJSONObject("win_combinations");
			boolean wins = false;
			BigDecimal totalreward = new BigDecimal("0");	
			for(int j = 0; j < 6; j++){
				String letter = null;
				switch(j){
					case 0:
						letter = "A";
						break;
					case 1:
						letter = "B";
						break; 
					case 2:
						letter = "C";
						break;
					case 3:
						letter = "D";
						break;
					case 4:
						letter = "E";
						break;
					default:
						letter = "F";
						break;
				}
		
				int maxcount = 0;
				String maxcountkey = null;
				JSONArray letterarray = new JSONArray();
				BigDecimal letterreward = dbetamount.multiply(new BigDecimal(String.valueOf(configObj.getJSONObject("symbols").getJSONObject(letter).getDouble("reward_multiplier"))));
				Iterator<String> keys = wincons.keys();
				while(keys.hasNext()) {
    					String key = keys.next();
					JSONObject jconf = wincons.getJSONObject(key);
					if(jconf.has("count")){
						int count = jconf.getInt("count");
						if(count < maxcount) //applied before
							continue;
				
						int lettercount = 0;
						boolean proved = false;	
						outerloop:			
						for(int r = 0; r < configObj.getInt("rows"); r++){
							for(int c = 0; c < configObj.getInt("columns"); c++){
								if(matrix[r][c].equals(letter)){
									lettercount++;
									if(lettercount == count){
										proved = true;
										break outerloop;	
									}
								}
							}
						}
						if(proved){
							wins = true;
							maxcount = count;
							maxcountkey = key;			
						}	
					}
					else if(jconf.has("covered_areas")){
						JSONArray covered = jconf.getJSONArray("covered_areas");
						boolean proved = true;
						outerloop2:	
						for(int r = 0; r < covered.length(); r++){
							JSONArray jrow = covered.getJSONArray(r);
							for(int c = 0; c < jrow.length(); c++){
								int row = Integer.parseInt(jrow.getString(c).split(":")[0]);
								int col = Integer.parseInt(jrow.getString(c).split(":")[1]);
								if(!matrix[row][col].equals(letter)){
									proved = false;
									break outerloop2;
								}
							}
						}
						if(proved){
							wins = true;
							letterarray.put(key);
							letterreward = letterreward.multiply(new BigDecimal(String.valueOf(jconf.getDouble("reward_multiplier"))));
						}					
					}
				}

				if(maxcountkey != null){
					wins = true;
					letterarray.put(maxcountkey);
					letterreward = letterreward.multiply(new BigDecimal(String.valueOf(wincons.getJSONObject(maxcountkey).getDouble("reward_multiplier"))));
				}

				if(!letterarray.isEmpty()){//if the letter wins
					totalreward = totalreward.add(letterreward);
					appliedwins.put(letter, letterarray);				
				}			

			}

			if(wins){
				resultjson.put("applied_winning_combinations", appliedwins);
				JSONArray appliedbonuses = new JSONArray();
				resultjson.put("applied_bonus_symbol", appliedbonuses);
				//look if a bonus exists in the generated matrix
				outloop3:
				for(int r = 0; r < configObj.getInt("rows"); r++){
					for(int c = 0; c < configObj.getInt("columns"); c++){
						switch(matrix[r][c]){
							case "10x":
								appliedbonuses.put("10x");
								totalreward = totalreward.multiply(new BigDecimal(String.valueOf(configObj.getJSONObject("symbols").getJSONObject("10x").getDouble("reward_multiplier"))));
								break outloop3;
							case "5x":
								appliedbonuses.put("5x");
								totalreward = totalreward.multiply(new BigDecimal(String.valueOf(configObj.getJSONObject("symbols").getJSONObject("5x").getDouble("reward_multiplier"))));
								break outloop3;
							case "+1000":
								appliedbonuses.put("+1000");
								totalreward = totalreward.add(new BigDecimal(String.valueOf(configObj.getJSONObject("symbols").getJSONObject("+1000").getDouble("extra"))));
								break outloop3;
							case "+500":
								appliedbonuses.put("+500");
								totalreward = totalreward.add(new BigDecimal(String.valueOf(configObj.getJSONObject("symbols").getJSONObject("+500").getDouble("extra"))));
								break outloop3;	
							case "MISS":
								appliedbonuses.put("MISS");
								break outloop3;
							default:
								break;		 	
						}
					}
				}
			}

			resultjson.put("reward", totalreward.intValue());
			return resultjson;
		}
}