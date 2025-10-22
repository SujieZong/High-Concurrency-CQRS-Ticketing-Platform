namespace CombinedShopService.Domain.Entities;

public class PriceTier
{
    public int PriceTierId { get; set; }

    public int EventId { get; set; }
    public Event? Event { get; set; }

    public required string TierName { get; set; }
    public decimal Price { get; set; }
    public string Currency { get; set; } = "CAD";
}