namespace CombinedShopService.Domain.Entities;


public class Category
{
    public int CategoryId { get; set; }
    public string CategoryName { get; set; }

    public string? Slug { get; set; } //for easier readable URL
}



