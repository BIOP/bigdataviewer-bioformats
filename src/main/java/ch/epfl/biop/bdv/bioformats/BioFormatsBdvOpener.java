package ch.epfl.biop.bdv.bioformats;

import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class BioFormatsBdvOpener {

    public static List<Source> openVolatile(File inputFile, String codeSerieChannel) {
        BioFormatsBdvSource.Opener opener = new BioFormatsBdvSource
                .Opener()
                .millimeter();
        return openVolatile(opener, inputFile, codeSerieChannel);
    }

    public static List<Source> openVolatile(BioFormatsBdvSource.Opener opener, File inputFile, String codeSerieChannel) {
        List<Source> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(inputFile,codeSerieChannel)
                .stream()
                .map(sc ->
                        sc.getRight().stream().map(
                                ch -> opener.file(inputFile).getVolatileSource(sc.getLeft(),ch)
                        ).collect(Collectors.toList())
                ).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return sources;
    }

    public static List<Source> openConcrete(File inputFile, String codeSerieChannel) {
        BioFormatsBdvSource.Opener opener = new BioFormatsBdvSource
                .Opener()
                .millimeter();
        return openConcrete(opener, inputFile, codeSerieChannel);
    }

    public static List<Source> openConcrete(BioFormatsBdvSource.Opener opener, File inputFile, String codeSerieChannel) {
        List<Source> sources = BioFormatsMetaDataHelper.getListOfSeriesAndChannels(inputFile,codeSerieChannel)
                .stream()
                .map(sc ->
                        sc.getRight().stream().map(
                                ch -> opener.file(inputFile).getConcreteSource(sc.getLeft(),ch)
                        ).collect(Collectors.toList())
                ).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return sources;
    }

}
