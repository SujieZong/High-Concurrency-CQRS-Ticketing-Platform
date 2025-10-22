namespace CombinedShopService.Domain.Entities;

public class Venue
{
    public int VenueId { get; set; }
    public required string VenueName { get; set; }
    public string? Address { get; set; }

    // 
    public int CityId { get; set; }
    public City? City { get; set; }
}