package command;

import org.reflections.Reflections;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class BuildDocumentation {
    static String doc = "";
    static String linkGitHubRepoPrefix = "https://github.com/BIOP/bigdataviewer_scijava/tree/master/src/main/java/";

    public static void main(String... args) {
        //

        Reflections reflections = new Reflections("ch.epfl.biop.bdv.bioformats");

        Set<Class<? extends Command>> commandClasses =
                reflections.getSubTypesOf(Command.class);

        HashMap<String, String> docPerClass = new HashMap<>();

        commandClasses.forEach(c -> {

            Plugin plugin = c.getAnnotation(Plugin.class);
            if (plugin!=null) {
                String url = linkGitHubRepoPrefix+c.getName().replaceAll("\\.","\\/")+".java";
                doc = "## [" + c.getSimpleName() + "]("+url+") [" + (plugin.menuPath() == null ? "null" : plugin.menuPath()) + "]\n";
                if (!plugin.label().equals(""))
                    doc+=plugin.label()+"\n";
                if (!plugin.description().equals(""))
                    doc+=plugin.description()+"\n";

                Field[] fields = c.getDeclaredFields();
                List<Field> inputFields = Arrays.stream(fields)
                                            .filter(f -> f.isAnnotationPresent(Parameter.class))
                                            .filter(f -> {
                                                Parameter p = f.getAnnotation(Parameter.class);
                                                return (p.type()==ItemIO.INPUT) || (p.type()==ItemIO.BOTH);
                                            }).collect(Collectors.toList());
                inputFields.sort(Comparator.comparing(f -> f.getName()));
                if (inputFields.size()>0) {
                    doc += "### Input\n";
                    inputFields.forEach(f -> {
                        doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                        if (!f.getAnnotation(Parameter.class).description().equals(""))
                            doc += f.getAnnotation(Parameter.class).description() + "\n";
                    });
                }

                List<Field> outputFields = Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.type()==ItemIO.OUTPUT) || (p.type()==ItemIO.BOTH);
                        }).collect(Collectors.toList());
                outputFields.sort(Comparator.comparing(f -> f.getName()));
                if (outputFields.size()>0) {
                    doc += "### Output\n";
                    outputFields.forEach(f -> {
                        doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                        if (!f.getAnnotation(Parameter.class).description().equals(""))
                            doc += f.getAnnotation(Parameter.class).description() + "\n";
                    });
                }

                doc+="\n";

                //System.out.println(doc);
                docPerClass.put(c.getName(),doc);
            }
        });
        Object[] keys = docPerClass.keySet().toArray();
        Arrays.sort(keys);
        for (Object key:keys) {
            String k = (String) key;
            System.out.println(docPerClass.get(k));
            //System.out.println(k);


        }

    }
}
