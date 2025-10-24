using CombinedShopService.Domain.Entities;
using CombinedShopService.Shared;

namespace CombinedShopService.Application;


public interface ICategoryApplication
{
    Task<Result<Category>> GetCategoryByIdAsync(int id, CancellationToken ct = default);

    Task<Result<List<Category>>> GetAllCategoriesAsync(CancellationToken ct = default);

    // get all games with category id
    Task<Result<List<EventDto>>> GetEventsByCategoryAsync(int categoryId);
}
