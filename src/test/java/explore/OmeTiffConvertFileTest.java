package explore;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.VolatileSpimSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.export.ometiff.OMETiffPyramidizerExporter;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import ch.epfl.biop.bdv.bioformats.imageloader.SeriesNumber;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import spimdata.util.Displaysettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OmeTiffConvertFileTest {

    static public void main(String... args) throws Exception {

        String path = "C:\\Users\\nicol\\Desktop\\";

        SourcesInfo sourcesInfo = getSourcesFromFile(path+"HyperStack-8bit.tif", 512, 521, 10, 0);

        OMETiffPyramidizerExporter.builder()
                .micrometer()
                //.lzw()
                .downsample(2)
                .maxTilesInQueue(10)
                .nThreads(0)
                .nResolutionLevels(2)
                .tileSize(512,512)
                .savePath(path+"HyperStack-8bit.ome.tif")
                .create(sourcesInfo.idToSources.get(0).toArray(new SourceAndConverter[0])).export();

    }

    public static SourcesInfo getSourcesFromFile(String path,int tileX,
                                                 int tileY,
                                                 int maxCacheSize,
                                                 int nParallelJobs) {
        BioFormatsBdvOpener opener =
                BioFormatsConvertFilesToSpimData.getDefaultOpener(path)
                        .micrometer()
                        .queueOptions(nParallelJobs, 4)
                        .cacheBlockSize(tileX, tileY, 1)
                        .cacheBounded(maxCacheSize*nParallelJobs);

        AbstractSpimData asd = BioFormatsConvertFilesToSpimData
                .getSpimData(
                        opener.voxSizeReferenceFrameLength(new Length(1, UNITS.MILLIMETER))
                                .positionReferenceFrameLength(new Length(1,UNITS.METER)));

        Map<Integer, SourceAndConverter> idToSource = new SourceAndConverterFromSpimDataCreator(asd).getSetupIdToSourceAndConverter();

        Map<Integer, SeriesNumber> idToSeries = new HashMap<>();
        Map<Integer, String> idToNames = new HashMap<>();

        idToSource.keySet().stream()
                .forEach(id -> {
                    BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(id);
                    SeriesNumber sn = bvs.getAttribute(SeriesNumber.class);
                    if (sn!=null) {
                        idToSeries.put(id, sn);
                        idToNames.put(id, sn.getName());
                    }
                    Displaysettings displaysettings = bvs.getAttribute(Displaysettings.class);
                    if (displaysettings!=null) {
                        Displaysettings.applyDisplaysettings(idToSource.get(id), displaysettings); // Applies color to sources
                    }
                });

        int nSources = idToSource.size();

        Map<Integer, List<SourceAndConverter>> idToSacs = new HashMap<>();

        for (int id = 0; id<nSources; id++) {
            SourceAndConverter source = idToSource.get(id);
            int sn_id = idToSeries.get(id).getId();
            if (!idToSacs.containsKey(sn_id)) {
                idToSacs.put(sn_id, new ArrayList<>());
            }
            idToSacs.get(sn_id).add(source);
        }

        SourcesInfo info = new SourcesInfo();

        info.idToSources = idToSacs;
        info.idToNames = idToNames;

        return info;
    }

    public static class SourcesInfo {
        public Map<Integer, List<SourceAndConverter>> idToSources;
        public Map<Integer, String> idToNames;
    }

    public static class SourceAndConverterFromSpimDataCreator  {
        private final AbstractSpimData asd;
        private final Map< Integer, SourceAndConverter> setupIdToSourceAndConverter;
        private final Map< SourceAndConverter, Map< String, Object > > sourceAndConverterToMetadata;

        public SourceAndConverterFromSpimDataCreator( AbstractSpimData asd )
        {
            this.asd = asd;
            setupIdToSourceAndConverter = new HashMap<>();
            sourceAndConverterToMetadata = new HashMap<>();
            createSourceAndConverters();
        }

        public Map< Integer, SourceAndConverter > getSetupIdToSourceAndConverter()
        {
            return setupIdToSourceAndConverter;
        }

        public Map< String, Object > getMetadata( SourceAndConverter< ? > sourceAndConverter)
        {
            return sourceAndConverterToMetadata.get( sourceAndConverter );
        }

        private void createSourceAndConverters()
        {
            boolean nonVolatile = WrapBasicImgLoader.wrapImgLoaderIfNecessary( asd );

            if ( nonVolatile )
            {
                System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
            }

            final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();

            final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();

            for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {

                final int setupId = setup.getId();

                ViewerSetupImgLoader vsil = imgLoader.getSetupImgLoader(setupId);

                String sourceName = createSetupName(setup);

                final Object type = vsil.getImageType();

                if (type instanceof RealType) {

                    createRealTypeSourceAndConverter( nonVolatile, setupId, sourceName );

                } else if (type instanceof ARGBType) {

                    createARGBTypeSourceAndConverter( setupId, sourceName );

                } else {
                    IJ.error("Cannot open Spimdata with Source of type " + type.getClass().getSimpleName());
                }

                sourceAndConverterToMetadata.put( setupIdToSourceAndConverter.get( setupId ), new HashMap<>() );

            }

            WrapBasicImgLoader.removeWrapperIfPresent( asd );
        }

        private void createRealTypeSourceAndConverter( boolean nonVolatile, int setupId, String sourceName )
        {
            final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

            Converter nonVolatileConverter = createConverterRealType((RealType)s.getType()); // IN FACT THE CASTING IS NECESSARY!!

            if (!nonVolatile ) {

                final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );

                Converter volatileConverter = createConverterRealType((RealType)vs.getType());

                setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));

            } else {

                setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter));
            }
        }

        private void createARGBTypeSourceAndConverter( int setupId, String sourceName )
        {
            final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
            final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

            Converter nonVolatileConverter = createConverterARGBType(s);
            if (vs!=null) {
                Converter volatileConverter = createConverterARGBType(vs);
                setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));
            } else {
                setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter));
            }
        }

        private static String createSetupName( final BasicViewSetup setup ) {
            if ( setup.hasName() ) {
                if (!setup.getName().trim().equals("")) {
                    return setup.getName();
                }
            }

            String name = "";

            final Angle angle = setup.getAttribute( Angle.class );
            if ( angle != null )
                name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

            final Channel channel = setup.getAttribute( Channel.class );
            if ( channel != null )
                name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

            if ((channel == null)&&(angle == null)) {
                name += "id "+setup.getId();
            }

            return name;
        }


        /**
         * Creates ARGB converter from a RealTyped sourceandconverter.
         * Supports Volatile RealTyped or non volatile
         * @param <T> realtype class
         * @param type a pixel of type T
         * @return a suited converter
         */
        public static< T extends RealType< T >>  Converter createConverterRealType( final T type ) {
            final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
            final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
            final RealARGBColorConverter< T > converter ;
            converter = RealARGBColorConverter.create(type, typeMin, typeMax );
            converter.setColor( new ARGBType( 0xffffffff ) );
            return converter;
        }

        /**
         * Creates ARGB converter from a RealTyped sourceandconverter.
         * Supports Volatile ARGBType or non volatile
         * @param source source
         * @return a compatible converter
         */
        public static Converter createConverterARGBType( Source source ) {
            final Converter converter ;
            if ( source.getType() instanceof Volatile)
                converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
            else
                converter = new ScaledARGBConverter.ARGB( 0, 255 );

            // Unsupported
            //converter.getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
            return converter;
        }

    }
}
