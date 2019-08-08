/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package free.rm.skytube.businessobjects.YouTube.VideoStream;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class NewPipeService {
    // TODO: remove this singleton
    private static NewPipeService instance;

    private final StreamingService streamingService;

    public NewPipeService( StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * Returns a list of video/stream meta-data that is supported by this app.
     *
     * @return List of {@link StreamMetaData}.
     */
    public StreamMetaDataList getStreamMetaDataListByUrl(String videoUrl) {
        StreamMetaDataList list = new StreamMetaDataList();

        try {

            // actual extraction
            StreamInfo streamInfo = StreamInfo.getInfo(streamingService, videoUrl);

            // now print the stream url and we are done
            for(VideoStream stream : streamInfo.getVideoStreams()) {
                list.add( new StreamMetaData(stream) );
            }
        } catch (ContentNotAvailableException exception) {
            list = new StreamMetaDataList(exception.getMessage());
        } catch (Throwable tr) {
            Logger.e(this, "An error has occurred while getting streams metadata.  URL=" + videoUrl, tr);
            list = new StreamMetaDataList(R.string.error_video_streams);
        }

        return list;
    }

    /**
     * Returns a list of video/stream meta-data that is supported by this app for this video ID.
     *
     * @return List of {@link StreamMetaData}.
     */
    public StreamMetaDataList getStreamMetaDataList(String videoId) {
        try {
            return getStreamMetaDataListByUrl(getVideoUrl(videoId));
        } catch (ParsingException e) {
            return new StreamMetaDataList(e.getMessage());
        }
    }

    public List<YouTubeVideo> getChannelVideos(String channelId) throws ExtractionException, IOException {
        // Get channel LinkHandler
        ListLinkHandler channelLink = streamingService.getChannelLHFactory().fromId("channel/" + channelId);

        // Extract from it
        ChannelExtractor channelExtractor = streamingService.getChannelExtractor(channelLink);
        channelExtractor.fetchPage();

        InfoItemsPage<StreamInfoItem> initialPage = channelExtractor.getInitialPage();
        List<YouTubeVideo> result = new ArrayList<>(initialPage.getItems().size());
        LinkHandlerFactory linkHandlerFactory = streamingService.getStreamLHFactory();
        for (StreamInfoItem item:initialPage.getItems()) {
            String id = linkHandlerFactory.getId(item.getUrl());
            result.add(new YouTubeVideo(id, item, channelExtractor));
        }
        Logger.i(this, "getChannelVideos for %s -> %s", channelId, result);
        return result;
    }

    public YouTubeVideo getDetails(String videoId) throws ExtractionException, IOException {
        LinkHandler url = streamingService.getStreamLHFactory().fromId(videoId);
        StreamExtractor extractor = streamingService.getStreamExtractor(url);
        extractor.fetchPage();

        String dateStr = extractor.getUploadDate();
        try {
            Date publishDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            return new YouTubeVideo(extractor.getId(), extractor.getName(), extractor.getDescription(),
                    extractor.getLength(), extractor.getLikeCount(), extractor.getDislikeCount(),
                    extractor.getViewCount(), publishDate);
        } catch (ParseException e) {
            throw new ExtractionException("Unable parse publish date:" + dateStr + " for video=" + videoId, e);
        }
    }

    /**
     * Given video ID it will return the video's page URL.
     *
     * @param videoId       The ID of the video.
     * @throws ParsingException 
     */
    private String getVideoUrl(String videoId) throws ParsingException {
        return streamingService.getStreamLHFactory().getUrl(videoId);
    }

    public synchronized static NewPipeService get() {
        if (instance == null) {
            instance = new NewPipeService(ServiceList.YouTube);
            SkyTubeApp.initNewPipe();
        }
        return instance;
    }
}
