using CombinedShopService.Domain.Entities;

namespace CombinedShopService.Domain.Dto
{
    public class EventDto
    {
        public int EventId { get; set; }
        public required string Title { get; set; }

        public DateTimeOffset StartTime { get; set; }
        public DateTimeOffset? EndTime { get; set; }

        public int CityId { get; set; }
        public string? CityName { get; set; }

        public int VenueId { get; set; }
        public string? VenueName { get; set; }

        public int CategoryId { get; set; }
        public string? CategoryName { get; set; }

        public IReadOnlyList<string>? Tags { get; set; }
        public IReadOnlyList<PriceTierDto>? PriceTiers { get; set; }

        public string? MetadataJson { get; set; }
    }
}