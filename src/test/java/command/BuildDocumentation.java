/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package command;

import org.reflections.Reflections;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.task.TaskService;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class BuildDocumentation {
    static String doc = "";
    static String linkGitHubRepoPrefix = "https://github.com/BIOP/bigdataviewer-bioformats/tree/master/src/main/java/";

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
                String[] paths = plugin.menuPath().split(">");
                doc = "## [" + paths[paths.length-1] + "]("+url+") \n";
                //" [" + (plugin.menuPath() == null ? "null" : plugin.menuPath()) + "]\n";
                if (!plugin.label().equals(""))
                    doc+=plugin.label()+"\n";
                if (!plugin.description().equals(""))
                    doc+=plugin.description()+"\n";

                Field[] fields = c.getDeclaredFields();
                List<Field> inputFields = Arrays.stream(fields)
                                            .filter(f -> f.isAnnotationPresent(Parameter.class))
                                            .filter(f -> {
                                                //Arrays.stream(f.getType().getInterfaces()).forEach(e -> System.out.println(e));
                                                return !Arrays.stream(f.getType().getInterfaces()).anyMatch(cl -> cl.equals(SciJavaService.class));
                                            })
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
