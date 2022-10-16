package com.example.localization;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents an Excel Reader object.
 */
public class ExcelReader {
    private InputStream in;
    private Set<iBeacon> allBeacons; // Stores all beacon objects retrieved from the Excel document

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int MAC_INDEX = 2;
    private static final int LONGITUDE_INDEX = 3;
    private static final int LATITUDE_INDEX = 4;
    private static final int FLOOR_INDEX = 5;

    /**
     * Disallowing use of empty constructor
     */
    private ExcelReader() {}

    /**
     * Constructor initializes this object with an input streamer and fetches all beacons
     * @param in Input stream object to read from
     */
    public ExcelReader(InputStream in) throws IOException {
        this.in = in;
        this.allBeacons = new HashSet<>();
        this.fetchAllBeacons();
    }

    /**
     * Fetches all the beacons from the excel file
     * @throws IOException
     */
    private void fetchAllBeacons() throws IOException {
        //Create Workbook instance holding reference to .xlsx file
        XSSFWorkbook workbook = new XSSFWorkbook(this.in);

        //Get first/desired sheet from the workbook
        XSSFSheet sheet = workbook.getSheetAt(0);

        //Iterate through each rows one by one
        for (Row row : sheet) {
            // Ignore first row
            if (row.getCell(ID_INDEX).getCellType() == CellType.STRING) continue;

            // Get values from Excel document
            int id = (int) row.getCell(ID_INDEX).getNumericCellValue();
            String name = row.getCell(NAME_INDEX).getStringCellValue();
            String mac = row.getCell(MAC_INDEX).getStringCellValue();
            double longitude = Double.parseDouble(row.getCell(LONGITUDE_INDEX).getStringCellValue());
            double latitude = Double.parseDouble(row.getCell(LATITUDE_INDEX).getStringCellValue());
            Location location = new Location(longitude, latitude);
            int floor = (int) row.getCell(FLOOR_INDEX).getNumericCellValue();

            // Create iBeacon object and add to set
            this.allBeacons.add(new iBeacon(id, name, mac, location, floor));
        }
    }

    public Set<iBeacon> getAllBeacons() {
        return this.allBeacons;
    }
}
