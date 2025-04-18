package shared.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.geldata.driver.Capabilities;
import com.geldata.driver.binary.PacketSerializer;
import com.geldata.driver.binary.protocol.common.Cardinality;
import com.geldata.driver.state.Session;

import shared.json.SessionDeserializer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class QueryExecutionArguments {
    public String name;

    public byte cardinality;
    public String value;
    public List<QueryParameter> arguments;
    public long capabilities;

    @JsonDeserialize(using = SessionDeserializer.class)
    public Session session;

    public Cardinality getCardinality() {
        return PacketSerializer.getEnumValue(Cardinality.class, cardinality);
    }

    public EnumSet<Capabilities> getCapabilities() {
        return EnumSet.copyOf(Arrays.stream(Capabilities.values())
                .map(Capabilities::getValue)
                .filter(v -> (v & capabilities) == v)
                .map(v -> PacketSerializer.getEnumValue(Capabilities.class, v))
                .collect(Collectors.toSet()));
    }
}
