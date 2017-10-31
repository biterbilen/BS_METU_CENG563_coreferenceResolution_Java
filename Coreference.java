 /************************
  *	Coreference.java
 *************************/
/*************************************************************************				
 *			CENG 563 TERM PROJECT 
 *				FOR
 *	COREFRENCE RESOLUTION IN MEDICAL JOURNAL ABSTRACTS
 *************************************************************************
 *				MIDPSL
 *				******
 *			     biter bilen
 *			      125001-8
 *************************************************************************/

import java.io.*; 
import java.util.*; 
// ---------------------------------------------------------------------
public class Coreference { 
	Vector coreferences = null;
	// ---------------------------------------------------------------------
	public void printAbstract(String infile) throws IOException, FileNotFoundException { 
		String line;
		System.out.println( "\nThe Abstract:");
		System.out.println( "--------------");
		File file = new File("okabstracts/"+infile+"a");
		BufferedReader in = new BufferedReader( new FileReader(file) );
		while ( (line=in.readLine()) != null ) { 
			System.out.println( line );
		}
		System.out.println( "" ); 
	} 
	// ---------------------------------------------------------------------
	void readSCOsResolveAnaphora(String infile) throws IOException, FileNotFoundException, InterruptedException {
		try { 
			printAbstract(infile);
		}
		catch (Exception e) { 
			System.err.println("Error in reading the Abstract"+e); 
			System.exit(0);
		}
		Vector referents = new Vector();
		Vector anaphor = new Vector();
		coreferences = new Vector();
		String line, token, sentence, referent, next;
		String det = "";
		sentence = new String(""); referent = new String("");
		token = new String("");
		File file = new File("okparses/"+infile);
		BufferedReader in = new BufferedReader( new FileReader(file) );
		char mode = 0;
		System.out.println("\nThe Anaphora in the Abstract in Appearance Order");
		System.out.println("------------------------------------------------");
		System.out.println("anaphor      ->      antecedent");
		System.out.println("-------------------------------");
		while ( (line=in.readLine()) != null ) {
			sentence += line.trim(); 
			if ( line.indexOf('.') > -1 ) { // sentence ends
				anaphor.clear(); 
				String tag="";
				StringTokenizer st = new StringTokenizer(sentence," )(",true);
				boolean dont_read = false; char num='s';
				while ( st.hasMoreTokens() || dont_read ) { 
					if (!dont_read) token = st.nextToken(); 
					dont_read = false;
					while (token.equals("(")) {
						token = st.nextToken();
						tag = token; 
					} 
					if (token.equals("DT")) {
						mode = 'd'; 
					} 
					// option 2: 
					else if ( token.equals("NN") || token.equals("NNS") || token.equals("NNP") ) {
						num = 's';
						if ( token.equals("NNS") ) num = 'p';
						token = st.nextToken(); // space ' '
						token = st.nextToken();
						if ( mode == 'd' ) { 
							if (referent.length()>0) referent += " "; 
							referent += token; 
							mode=0; 
							if ( det.equals("the") || det.equals("The") || det.equals("This") || det.equals("this") || det.equals("these") || det.equals("These") ) {
								SCO s = new SCO(referent);
								s.head = token;
								s.number = num;
								s.ltpos = 0.5f+referents.size();
								anaphor.add( s );
								referent = ""; 
							}
						}
						else referent = token; 
					} 
					else if ( !token.equals(")") && !token.equals(" ") && st.hasMoreTokens() ) {
						next = st.nextToken();
						if ( next.equals(")") ) { 
							// token is a word in the sentence 
							if ( mode == 'd' ) { 	
								if (referent.length()>0) referent += " "; 
								referent += token; 
							} 
							if ( tag.equals("DT") ) { 
								det = token; 
							} 
							else if ( tag.equals("PRP") || tag.equals("PRP$") ) { 
								SCO a = new SCO(token);
								a.head = token;
								a.ltpos = 0.5f+referents.size();
								if ( token.equals("they") || token.equals("they're") || token.equals("them") || token.equals("their")) { 
									a.number = 'p';
								}
								anaphor.add( a ); 
							} // if(PRP)
						}
						else { 
							token = next; 
							dont_read = true;
						} 
					} 
					if ( mode==0 && referent.length()>0 ) { 
						SCO r = new SCO(referent);
						r.head = token; // coming from option 2
						r.number = num;
						r.ltpos = referents.size();
						referents.add( r );
						referent = ""; 
					}
				}
				// print out the SCO
				System.out.println(referent);
				sentence = ""; 
				// resolve anaphoric links saving the antecedents
				for (int i=0; i<anaphor.size(); i++) { 
					SCO an = (SCO)anaphor.elementAt(i); 
					float best_prior = 100; 
					SCO best_SCO = null; 
					for (int j=referents.size()-1; j>=0; j--) {
						SCO rf = (SCO)referents.elementAt(j); 
						float prior = Math.abs(rf.ltpos-an.ltpos); 
						if ( rf.number == an.number ) { 
							if ( prior<=best_prior ) { 
								best_SCO = rf; 
								best_prior = prior; 
							} 
						} 
					} 
					if ( best_SCO == null ) {
						System.out.println( " " + an.phrase + " -> unknown!" ); 
					} 
					else { 
						Vector ar = new Vector();
						ar.addElement(anaphor.elementAt(i));
						ar.addElement(best_SCO);
						coreferences.addElement(ar);
						
						System.out.println( " " + an.phrase 
								+ " -> " + best_SCO.phrase ); 
					} 
				} 
			} // if sentence ends
						
		} // while
	} // readSCOsResolveAnaphora() 
	public void printCoreferences() {
		System.out.println("\nThe Coreference Chains:"); 
		System.out.println("-----------------------"); 
		int cnt = coreferences.size();
		SCO o2,o1;
		
		
		Vector ante = new Vector();
		for (int i=0; i<cnt; i++) {
			o2 = new SCO ((SCO)((Vector)coreferences.elementAt(i)).elementAt(1));
			ante.addElement(o2);
		}
		
		Vector index = null;
		for (int i=0; i<cnt; i++) {
			index = new Vector();
			index.addElement(new Integer(i));
			for(int j=i+1; j<cnt; j++) {
				if (((SCO)ante.elementAt(i)).equals((SCO)ante.elementAt(j))) {
					index.addElement(new Integer(j));
				}
			}
			if (index.size() > 1) {
				o2 = new SCO ((SCO)((Vector)coreferences.elementAt(i)).elementAt(1));
				System.out.print("\nAntecedent -");
				o2.print();
				System.out.println("linked the following anaphors:");
				for (int k=0; k<index.size(); k++) {
					Integer ind = new Integer (0);
					ind = (Integer) index.get(k);
					int iind = ind.intValue();
					o1 = new SCO ((SCO)((Vector) coreferences.elementAt(iind)).elementAt(0));
					o1.print();
				}
			}
		}
		System.out.println("");
		
		
	} // printCoreferences
	public static void main(String args[]) {
		Coreference o = new Coreference();
		try { 
			o.readSCOsResolveAnaphora(args[0]);
			o.printCoreferences();
			
		} 
		catch (Exception e) { 
			System.err.println("ERROR -\n"+e); 
			System.exit(0);
		} 
	} 
} // class Coreference
// ---------------------------------------------------------------------
class SCO extends Object { 
	String phrase; // sytactic chunk object
	String head;   // the noun or verb
	char number;   // 's' for single, 'p' for plural
	float ltpos; 
	
	public SCO(SCO o) {
		this.phrase = o.phrase;
		this.head = o.head;
		this.number = o.number;
	} 
	public SCO(String p) {
	// default values
		phrase = p;
		number = 's';
	} 
	public void print() {
		System.out.println(" "+ phrase ); 
	} 
	public boolean equals(SCO o) {
		if (this.phrase.equals(o.phrase) && 
			this.head.equals(o.head) && 
			this.number==o.number)
			return true;
		return false;
	}
} 
// ---------------------------------------------------------------------
