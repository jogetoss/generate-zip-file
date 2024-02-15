/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.joget.generateZipFile;

import java.util.Map;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joget.apps.form.service.FileUtil;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.joget.apps.app.service.AppPluginUtil;

import org.joget.commons.util.LogUtil;
/**
 *
 * @author Maxson
 */
public class GenerateZipTool extends DefaultApplicationPlugin{
    
    private final static String MESSAGE_PATH = "message/form/GenerateZipTool";

    @Override
    public Object execute(Map props) {
        
        String recordId = getPropertyString("formRecordId");
        String formDefId = getPropertyString("formDefId");
        String formField = getPropertyString("formField");
        String targetFormField = getPropertyString("targetFormField");
        String targetFormId = getPropertyString("targetFormId");
        String targetRecordId = getPropertyString("targetRecordId");
        String zipFileName = getPropertyString("zipFileName");
        String password = getPropertyString("zipPassword");
        
        ApplicationContext ac = AppUtil.getApplicationContext();
        AppService appService = (AppService) ac.getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        
        String path = FileUtil.getUploadPath( appService.getFormTableName(appDef, targetFormId) , targetRecordId);
//        zipFileName = path + zipFileName + ".zip"; 
        zipFileName += ".zip";
        File newZipFile = new File(path + zipFileName);
        
        try{
            FormRowSet set = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, recordId);
            FormRow row = set.get(0);
            
            //Get excel file
            if(!row.get(getPropertyString("formField")).toString().isEmpty()){
                //Get files name
                String filesName = row.get(getPropertyString("formField")).toString();
                String[] fileNames = filesName.split(";");
                
                //Validate if password is provided 
                if(!password.isEmpty()){
                    System.out.println("Password provided");
                    
                    //Define Zip Parameters
                    ZipParameters zipParameters = new ZipParameters();
                    zipParameters.setEncryptFiles(true);
                    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
                    
                    try{
                        ZipFile zipFile = new ZipFile(newZipFile , password.toCharArray());                        
                        
                        
                        // Add files to the zip file
                        for (String fileName : fileNames) {
                            File file = FileUtil.getFile(fileName.trim(), appService.getFormTableName(appDef, formDefId), recordId);
                            if (file != null && file.exists()) {
                                zipFile.addFile(file, zipParameters);
                            } else {
                                System.out.println("File not found: " + fileName);
                            }
                        }
                            System.out.println("Files have been zipped successfully!");
                        
                            //get target form row
                            FormRowSet targetSet = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), targetFormId, targetRecordId);
                            FormRow targetRow = targetSet.get(0); 

                            //Add Zip File into selected form field
                            if(!targetRow.get(getPropertyString("targetFormField")).toString().isEmpty()){
                                targetRow.put(getPropertyString("targetFormField"), targetRow.get(getPropertyString("targetFormField")) + ";" + zipFileName);
                            }else{
                                targetRow.put(getPropertyString("targetFormField"), zipFileName);
                            }
                            targetSet.remove(0); 
                            targetSet.add(0, targetRow);
                            appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), targetFormId, targetSet, targetRecordId);
                            System.out.println("Zip File Generated Successfully for [" + targetRecordId + "]");                        

                    }catch(Exception e){
                        LogUtil.error("Zip File (password)", e, e.getMessage());
                    }
                    
                }else{
                    System.out.println("No password provided");
                    //Zip all excel files into 1 zip file
                    try (FileOutputStream fos = new FileOutputStream(newZipFile);
                        ZipOutputStream zos = new ZipOutputStream(fos)) {

                        // Buffer for reading the file
                        byte[] buffer = new byte[1024];

                            // Iterate over each file name
                            for (String fileName : fileNames) {
                                // Get the file
                                File file = FileUtil.getFile(fileName.trim(), appService.getFormTableName(appDef, formDefId), recordId);

                                if (file != null && file.exists()) {
                                    // Add a new entry to the zip file
                                    ZipEntry zipEntry = new ZipEntry(fileName.trim());
                                    zos.putNextEntry(zipEntry);

                                    // Read the file and write it to the zip output stream
                                    try (FileInputStream fis = new FileInputStream(file)) {
                                        int length;
                                        while ((length = fis.read(buffer)) > 0) {
                                            zos.write(buffer, 0, length);
                                        }
                                    }
                                    // Close the current entry
                                    zos.closeEntry();
                                } else {
                                    System.out.println("File not found: " + fileName);
                                }
                            }
                            System.out.println("Files have been zipped successfully!");

                            //get target form row
                            FormRowSet targetSet = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), targetFormId, targetRecordId);
                            FormRow targetRow = targetSet.get(0); 

                            //Add Zip File into selected form field
                            if(!targetRow.get(getPropertyString("targetFormField")).toString().isEmpty()){
                                targetRow.put(getPropertyString("targetFormField"), targetRow.get(getPropertyString("targetFormField")) + ";" + zipFileName);
                            }else{
                                targetRow.put(getPropertyString("targetFormField"), zipFileName);
                            }
                            targetSet.remove(0); 
                            targetSet.add(0, targetRow);
                            appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), targetFormId, targetSet, targetRecordId);
                            System.out.println("Zip File Generated Successfully for [" + targetRecordId + "]");

                        }catch (IOException e) {
                            LogUtil.error("Zip File (no-password)", e, e.getMessage());
                        } 
                    }
    
                }else{
                    System.out.println("Form Row not found for ID: " + recordId);
                }
            
        }catch(Exception e){
            LogUtil.error("App - recordID", e, "Failed to generate Zip File for [" + targetRecordId + "]");
        }
        
        
        return null;
    }

    @Override
    public String getName() {
        return "Generate Zip Tool";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.GenerateZipTool.pluginDesc" , getClassName() , MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.GenerateZipTool.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/form/generateZipTool.json", null, true, MESSAGE_PATH);
    }
    
    
}
