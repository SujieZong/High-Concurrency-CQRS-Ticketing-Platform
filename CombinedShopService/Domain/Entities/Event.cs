namespace CombinedShopService.Domain.Entities;

public class Event
{
    public int EventId { get; set; }
    public required string Title { get; set; }

    //Time start and End
    public DateTimeOffset StartTime { get; set; }
    public DateTimeOffset? EndTime { get; set; }

    public int CityId { get; set; }
    public int VenueId { get; set; }
    public int CategoryId { get; set; }
    public int TagId { get; set; }


    public City? City { get; set; } //Navigation Property
    public Venue? Venue { get; set; }
    public Category? Category { get; set; }


    private readonly List<PriceTier> _priceTiers = new();
    public IReadOnlyCollection<PriceTier> PriceTiers => _priceTiers.AsReadOnly();

    // other info Singer, Host...
    public string? MetadataJson { get; set; }

}