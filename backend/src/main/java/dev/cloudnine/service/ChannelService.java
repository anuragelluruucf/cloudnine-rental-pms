package dev.cloudnine.service;

import dev.cloudnine.domain.ChannelSource;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.ChannelSourceRepository;
import dev.cloudnine.web.ApiException;
import dev.cloudnine.web.Dtos.ChannelRequest;
import dev.cloudnine.web.Dtos.ChannelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChannelService {

    private final ChannelSourceRepository channels;
    private final PropertyService properties;

    public ChannelService(ChannelSourceRepository channels, PropertyService properties) {
        this.channels = channels;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> list(UserAccount actor, UUID propertyId) {
        properties.requireReadable(actor, propertyId);
        return channels.findByPropertyIdOrderByChannelAsc(propertyId).stream().map(ChannelService::toResponse).toList();
    }

    @Transactional
    public ChannelResponse create(UserAccount actor, UUID propertyId, ChannelRequest request) {
        var property = properties.requireReadable(actor, propertyId);
        if (channels.findByPropertyIdAndChannel(propertyId, request.channel()).isPresent()) {
            throw ApiException.conflict("CHANNEL_EXISTS", "That channel is already connected.");
        }
        ChannelSource channel = new ChannelSource();
        channel.setProperty(property);
        channel.setChannel(request.channel());
        channel.setImportUrl(blankToNull(request.importUrl()));
        channel.setEnabled(true);
        return toResponse(channels.save(channel));
    }

    @Transactional
    public ChannelResponse update(UserAccount actor, UUID channelId, ChannelRequest request) {
        ChannelSource channel = channels.findById(channelId)
                .orElseThrow(() -> ApiException.notFound("Channel not found"));
        properties.requireReadable(actor, channel.getProperty().getId());
        channel.setImportUrl(blankToNull(request.importUrl()));
        channel.setEnabled(true);
        return toResponse(channel);
    }

    public ChannelSource require(UserAccount actor, UUID channelId) {
        ChannelSource channel = channels.findById(channelId)
                .orElseThrow(() -> ApiException.notFound("Channel not found"));
        properties.requireReadable(actor, channel.getProperty().getId());
        return channel;
    }

    static ChannelResponse toResponse(ChannelSource channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getProperty().getId(),
                channel.getChannel(),
                channel.getImportUrl(),
                channel.getLastSyncedAt(),
                channel.getLastSyncMessage(),
                channel.isEnabled()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
