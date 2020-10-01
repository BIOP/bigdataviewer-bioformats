package ch.epfl.biop.bdv.bioformats;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.state.SourceGroup;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsSetupLoader;
import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */

@Deprecated
public class BioformatsBdvDisplayHelper {

    public static void autosetColorsAngGrouping(List<BdvStackSource<?>> lbss, AbstractSpimData asd, boolean setColor, double minValue, double maxValue, boolean setGrouping) {
        BdvHandle bdv_h = lbss.get(0).getBdvHandle();

        // Set Color to each channel
        if (setColor) {
            asd.getSequenceDescription().getViewSetupsOrdered().forEach(id_vs -> {
                        int idx = ((BasicViewSetup) id_vs).getId();
                        BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) asd.getSequenceDescription().getImgLoader().getSetupImgLoader(idx);
                        lbss.get(idx).setColor(
                                BioFormatsMetaDataHelper.getSourceColor((BioFormatsBdvSource) bfsl.concreteSource)
                        );
                        lbss.get(idx).setDisplayRange(minValue, maxValue);
                    }
            );
        }

        if (setGrouping) {
            // Group source index per channel
            Map<BioFormatsMetaDataHelper.BioformatsChannel, List<Integer>> srcsGroupedByChannel =
                    (Map<BioFormatsMetaDataHelper.BioformatsChannel, List<Integer>>)
                            asd.getSequenceDescription()
                                    .getViewSetupsOrdered().stream()
                                    .map(obj -> ((BasicViewSetup) obj).getId())
                                    .collect(Collectors.groupingBy(e -> {
                                                try {
                                                    BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) asd.getSequenceDescription().getImgLoader().getSetupImgLoader((int) e);
                                                    IFormatReader reader = bfsl.getReaderPool().acquire();

                                                    // TODO : fix series!!

                                                    BioFormatsMetaDataHelper.BioformatsChannel channel = new BioFormatsMetaDataHelper.BioformatsChannel((IMetadata) reader.getMetadataStore(), bfsl.iSerie, bfsl.iChannel, reader.isRGB());

                                                    bfsl.getReaderPool().recycle(reader);
                                                    return channel;
                                                } catch (Exception exception) {
                                                    exception.printStackTrace();
                                                    return null;
                                                }
                                            },
                                            Collectors.toList()));


            Map<Integer, BioFormatsMetaDataHelper.BioformatsChannel> idToChannel = new HashMap<>();

            srcsGroupedByChannel.entrySet().stream().forEach(e -> {
                e.getValue().forEach(id -> idToChannel.put(id,e.getKey()));
            });

            List<BioFormatsMetaDataHelper.BioformatsChannel> orderedChannelList = new ArrayList<>();

            asd.getSequenceDescription()
                    .getViewSetupsOrdered().stream()
                    .map(obj -> ((BasicViewSetup) obj).getId()).forEach(
                            id -> {
                                if (!orderedChannelList.contains(idToChannel.get(id))) {
                                    orderedChannelList.add(idToChannel.get(id));
                                }
                            }
            );

            List<SourceGroup> sgs =
                    orderedChannelList.stream().map(
                    ch -> {
                        SourceGroup sg = new SourceGroup(ch.chName);
                        srcsGroupedByChannel.get(ch).forEach(idx -> sg.addSource(idx));
                        return sg;
                    }
            ).collect(Collectors.toList());

            // Group source per channel
            int idx = 0;
            while (idx<sgs.size()) {
                final int idx_cp = idx;
                if (idx<bdv_h.getViewerPanel().getVisibilityAndGrouping().getSourceGroups().size()) {
                    SourceGroup sg = bdv_h.getViewerPanel()
                            .getVisibilityAndGrouping()
                            .getSourceGroups().get(idx);

                    sg.setName(sgs.get(idx).getName());
                    sgs.get(idx).getSourceIds().stream().forEach(
                            id -> {
                                sg.addSource(id);
                                bdv_h.getSetupAssignments().moveSetupToGroup(bdv_h.getSetupAssignments().getConverterSetups().get(id),
                                        bdv_h.getSetupAssignments().getMinMaxGroups().get(idx_cp));
                            }
                    );

                } else {
                    bdv_h.getViewerPanel().addGroup(sgs.get(idx));
                    sgs.get(idx).getSourceIds().stream().forEach(
                            id -> {
                                bdv_h.getSetupAssignments().moveSetupToGroup(bdv_h.getSetupAssignments().getConverterSetups().get(id),
                                        bdv_h.getSetupAssignments().getMinMaxGroups().get(idx_cp));
                            }
                    );
                }
                idx++;
            }

            // Creates group by datalocation
            Map<String, ArrayList<Integer>> dataLocationToViewSetup =
                    (Map<String, ArrayList<Integer>>)
                            asd.getSequenceDescription().getViewSetupsOrdered().stream()
                                    .map(obj -> ((BasicViewSetup) obj).getId())
                                    .collect(Collectors.groupingBy(e -> {
                                                BioFormatsSetupLoader bfsl = (BioFormatsSetupLoader) asd.getSequenceDescription().getImgLoader().getSetupImgLoader((int)e);
                                                return bfsl.getOpener().dataLocation;
                                            },
                                            Collectors.toList()));

            /*List<BioFormatsMetaDataHelper.BioformatsChannel> orderedDatLocationList = new ArrayList<>();

            asd.getSequenceDescription()
                    .getViewSetupsOrdered().stream()
                    .map(obj -> ((BasicViewSetup) obj).getId()).forEach(
                    id -> {
                        if (!orderedChannelList.contains(idToChannel.get(id))) {
                            orderedChannelList.add(idToChannel.get(id));
                        }
                    }
            );*/

            sgs = dataLocationToViewSetup.entrySet().stream().map(
                    e -> {
                        SourceGroup sg = new SourceGroup(e.getKey());
                        e.getValue().forEach(index -> sg.addSource(index));
                        return sg;
                    }
            ).collect(Collectors.toList());



            int idx_offs = idx;
            while (idx<sgs.size()+idx_offs) {
                if (idx<bdv_h.getViewerPanel().getVisibilityAndGrouping().getSourceGroups().size()) {
                    SourceGroup sg = bdv_h.getViewerPanel()
                            .getVisibilityAndGrouping()
                            .getSourceGroups().get(idx);

                    sg.setName(sgs.get(idx-idx_offs).getName());
                    sgs.get(idx-idx_offs).getSourceIds().stream().forEach( id -> sg.addSource(id) );
                } else {
                    bdv_h.getViewerPanel().addGroup(sgs.get(idx-idx_offs));
                }
                idx++;
            }

            // dirty but updates display - update do not have a public access
            SourceGroup dummy = new SourceGroup("dummy");
            bdv_h.getViewerPanel().addGroup(dummy);
            bdv_h.getViewerPanel().removeGroup(dummy);

        }

    }

}
