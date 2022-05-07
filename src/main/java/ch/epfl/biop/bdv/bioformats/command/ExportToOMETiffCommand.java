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
package ch.epfl.biop.bdv.bioformats.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.export.ometiff.OMETiffExporter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BigDataViewer-Playground>Sources>Export>Export Sources To OME Tiff",
        description = "Saves Bdv sources as a multi-channel OME-Tiff file, keeping original multiresolution levels"+
                " if the sources are initially multiresolution.")
public class ExportToOMETiffCommand implements Command {

    @Parameter(label = "Sources to export")
    public SourceAndConverter[] sacs;

    @Parameter(label = "Output file", style = "save")
    public File file;

    @Parameter( label = "Physical unit", choices = {"MILLIMETER", "MICROMETER"})
    String unit;

    @Parameter( label = "Tile Size X (negative: no tiling)")
    int tile_size_x = 512;

    @Parameter( label = "Tile Size Y (negative: no tiling)")
    int tile_size_y = 512;

    @Parameter( label = "Number of threads (0 = serial)")
    int n_threads = 8;

    @Parameter( label = "Number of tiles computed in advance")
    int max_tiles_queue = 256;

    @Parameter( label = "Use LZW compression")
    Boolean lzw_compression = false;

    @Parameter
    TaskService taskService;

    @Override
    public void run() {

        List<SourceAndConverter> sources = Arrays.asList(sacs);

        sacs = sources.toArray(new SourceAndConverter[0]);

        Task task = taskService.createTask("Export: "+file.getName());

        OMETiffExporter.Builder builder = OMETiffExporter
                .builder()
                .monitor(task)
                .savePath(file.getAbsolutePath());

        if (lzw_compression) builder.lzw();
        if (unit.equals("MILLIMETER")) builder.millimeter();
        if (unit.equals("MICROMETER")) builder.micrometer();
        if ((tile_size_x>0)&&(tile_size_y >0)) builder.tileSize(tile_size_x, tile_size_y);
        builder.maxTilesInQueue(max_tiles_queue);
        builder.nThreads(n_threads);

        try {
            builder.create(sacs).export();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
