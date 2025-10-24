namespace CombinedShopService.Domain.Dto;

public class PriceTierDto
{
    public int PriceTierId { get; set; }
    public required string TierName { get; set; }
    
    public int EventId { get; set; }
    public Event? Event { get; set; }

    public decimal Price { get; set; }
    public string Currency { get; set; } = "CAD";
}