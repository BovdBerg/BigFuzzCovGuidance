import P2.CommuteType.Spec_BigFuzz.CommuteType;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RunWith(JQF.class)

public class CommuteTypeDriver {

    @Fuzz
    public void testCommuteType(String fileName) throws IOException {
        List<String> fileList = Files.readAllLines(Paths.get(fileName));
        CommuteType analysis = new CommuteType();
        analysis.CommuteType(fileList.get(0), fileList.get(1));
    }

    public static void main(String[] args) throws IOException {
        CommuteType commuteType = new CommuteType();
        commuteType.CommuteType("trips.csv","zipcode.csv");
    }
}