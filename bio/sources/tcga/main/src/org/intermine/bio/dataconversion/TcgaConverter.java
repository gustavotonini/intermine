package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;


/*import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;*/


import java.io.IOException;
import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;



/**
 * 
 * @author
 */
public class TcgaConverter extends BioFileConverter
{
	
	private static final Logger LOG = Logger.getLogger(TcgaConverter.class);

    //
    private Item sub;
    private Item org;
    private static final String DATASET_TITLE =
        "TCGA";
    private static final String DATA_SOURCE_NAME = "TCGA";


    private static final String HOMOSAPIENS_TAX_ID = "9606";
	
	private static Map<String, Item> patients = null;
	private static Map<String, Item> aliquots = null;


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public TcgaConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
		createOrganismItem();
    }
	

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
		// There initially are four files:
        // - nationwidechildrens.org_clinical_patient_ov.txt -
		//patient description file
        // estimated expression levels for annotated genes
        // The following code works out which file we are reading and calls the corresponding method
        File currentFile = getCurrentFile();

        if ("1_nationwidechildrens.org_clinical_patient_ov.txt"
                .equals(currentFile.getName())) {
            processPatientsFile(reader, sub, org);
        } else if ("2_nationwidechildrens.org_clinical_drug_ov.txt"
                .equals(currentFile.getName())) {
            processClinical(reader, sub, org, "TCGAClinicalDrug");
        } else if ("3_nationwidechildrens.org_clinical_follow_up_v1.0_ov.txt"
                .equals(currentFile.getName())) {
            processClinical(reader, sub, org, "TCGAClinicalFollowUp");
        } else if ("3_nationwidechildrens.org_clinical_follow_up_v1.0_nte_ov.txt"
                .equals(currentFile.getName())) {
            processClinical(reader, sub, org, "TCGAClinicalFollowUpNTE");
        } else if ("3_nationwidechildrens.org_clinical_radiation_ov.txt"
                .equals(currentFile.getName())) {
            processClinical(reader, sub, org, "TCGAClinicalRadiation");
        } else if ("3_nationwidechildrens.org_biospecimen_aliquot_ov.txt"
                .equals(currentFile.getName())) {
            processAliquot(reader, sub, org);
        } else if (currentFile.getName().matches("4_.*GenomeWideSNP.*.seg.v2.txt") ) {
            processCNV(reader, sub, org);			
        } else {
            throw new IllegalArgumentException("Unexpected file: "
                    + currentFile.getName());
        }
    }
	
	/**
     * Create and store a organism item on the first time called.
     *
     * @throws ObjectStoreException os
     */
    protected void createOrganismItem() throws ObjectStoreException {
        org = createItem("Organism");
        org.setAttribute("taxonId", HOMOSAPIENS_TAX_ID);
        store(org);
    }

	private void processCNV(Reader reader, Item submission, Item organism)
		throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;
		Item patient = null;

        //drugs = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
				
            } else {
                String aliquotUUID = line[0]; //bcr_aliquot_uuid
				patient = aliquots.get(aliquotUUID);
				
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(aliquotUUID) || (patient==null)) {
                    break;
                }
				
				
                Item cnv = createItem("TCGACNV");

                // extract informations
                for (int i = 0; i < headers.length; i++) {
                    String col = headers[i];
					cnv.setAttribute(col, line[i]);
                }
				cnv.setReference("patient", patient);
				//cnv.addCollection("genes", patient);
                store(cnv);
                
            }
            lineNumber++;
		}
	}
	

	private void processAliquot(Reader reader, Item submission, Item organism)
		throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;
		Item patient = null;

        //drugs = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
				
				//skip one useless line following the header
				line = (String[]) tsvIter.next();
				lineNumber +=1;
				
            } else {
                String patientUUID = line[0]; //bcr_patient_uuid
				if (lineNumber == 2) {
					//load patient
					patient = patients.get(patientUUID);
				}
                String aliquotUUID = line[3]; //bcr_aliquot_uuid
				
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(patientUUID) || StringUtils.isEmpty(aliquotUUID) || (patient==null)) {
                    break;
                }
				aliquots.put(aliquotUUID, patient);

            }
            lineNumber++;
		}
	}
	
	
	private void processClinical(Reader reader, Item submission, Item organism, String type)
		throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;

        //drugs = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
				
				//skip two useless lines following the header
				line = (String[]) tsvIter.next();
				line = (String[]) tsvIter.next();
				lineNumber +=2;
				
            } else {
                String patientUUID = line[0]; //bcr_patient_uuid
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(patientUUID)) {
                    break;
                }
                Item item = createItem(type);
				//Item patient = patients.get(patientUUID);

                // extract informations
                for (int i = 0; i < headers.length; i++) {
                    String col = headers[i];
					item.setAttribute(col, line[i]);
                }
				item.setReference("patient", patients.get(patientUUID));
                store(item);

            }
            lineNumber++;
		}
	}
		
	private void processPatientsFile(Reader reader, Item submission, Item organism)
        throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;

        patients = new HashMap<String, Item>();
        aliquots = new HashMap<String, Item>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
				
				//skip two useless lines following the header
				line = (String[]) tsvIter.next();
				line = (String[]) tsvIter.next();
				lineNumber +=2;
				
            } else {
                String patientUUID = line[0]; //bcr_patient_uuid
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(patientUUID)) {
                    break;
                }
                Item patient = createItem("TCGAPatient");

				
                // extract informations
                for (int i = 0; i < headers.length; i++) {
                    String col = headers[i];
					patient.setAttribute(col, line[i]);
                }
				//patient.setReference("followups", organism);
                store(patient);
				patients.put(patientUUID, patient);

            }
            lineNumber++;
        }
    }

}
